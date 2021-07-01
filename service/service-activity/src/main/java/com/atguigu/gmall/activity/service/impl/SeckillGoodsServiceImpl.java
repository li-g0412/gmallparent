package com.atguigu.gmall.activity.service.impl;

import com.atguigu.gmall.activity.mapper.SeckillGoodsMapper;
import com.atguigu.gmall.activity.service.SeckillGoodsService;
import com.atguigu.gmall.activity.util.CacheHelper;
import com.atguigu.gmall.common.constant.RedisConst;
import com.atguigu.gmall.common.result.Result;
import com.atguigu.gmall.common.result.ResultCodeEnum;
import com.atguigu.gmall.common.util.MD5;
import com.atguigu.gmall.model.activity.OrderRecode;
import com.atguigu.gmall.model.activity.SeckillGoods;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * @author atguigu-mqx
 */
@Service
public class SeckillGoodsServiceImpl implements SeckillGoodsService {

    //  查询mapper? 但是，我们只需要查询缓存！
    @Autowired
    private RedisTemplate redisTemplate;

    @Autowired
    private SeckillGoodsMapper seckillGoodsMapper;

    @Override
    public List<SeckillGoods> findAll() {
        //  获取缓存中的所有秒杀商品列表 hvals key
        List<SeckillGoods> seckillGoodsList = redisTemplate.boundHashOps(RedisConst.SECKILL_GOODS).values();
        return seckillGoodsList;
    }

    @Override
    public SeckillGoods getSeckillGoods(Long skuId) {
        //  hget key field
        SeckillGoods seckillGoods = (SeckillGoods) redisTemplate.boundHashOps(RedisConst.SECKILL_GOODS).get(skuId.toString());
        return seckillGoods;
    }

    @Override
    public void seckillOrder(Long skuId, String userId) {
        /*
        a.	判断产品的状态位！
        b.  用户是否已经下过订单
        c.  减库存，如果发现没有数据了，则通知其他兄弟节点！
                需要将数据再保存到缓存一份！
        d.  更新库存 redis,mysql!
         */
        //  校验状态位：
        String status = (String) CacheHelper.get(skuId.toString());
        if (StringUtils.isEmpty(status) || "0".equals(status)){
            //  验证产品的状态位失败！
            return;
        }
        //  判断用户是否下过订单 key = seckill:user:userId;
        //  setnx key value
        String seckillKey = RedisConst.SECKILL_USER + userId;
        //  将用户信息保存到缓存中 {哪个用户，秒杀的哪件商品}
        Boolean flag = redisTemplate.opsForValue().setIfAbsent(seckillKey, skuId, RedisConst.SECKILL__TIMEOUT, TimeUnit.SECONDS);
        //  判断 用户已经下过订单！
        if (!flag){
            return;
        }
        //  减少库存 redis-list 存储的数据！
        //  key = seckill:stock:46
        String stockKey = RedisConst.SECKILL_STOCK_PREFIX + skuId;
        //  redisTemplate.opsForList().leftPush(key,seckillGoods.getSkuId());
        String rpopSkuId = (String) redisTemplate.boundListOps(stockKey).rightPop();
        //  判断获取到的数据
        if (StringUtils.isEmpty(rpopSkuId)){
            //  通知其他兄弟节点
            redisTemplate.convertAndSend("seckillpush",skuId+":0");
            return;
        }

        //  将有关于订单的数据保存到缓存一份！
        OrderRecode orderRecode = new OrderRecode();
        orderRecode.setNum(1);
        orderRecode.setUserId(userId);
        //  订单码
        orderRecode.setOrderStr(MD5.encrypt(userId+skuId));
        //  保存秒杀商品对象
        orderRecode.setSeckillGoods(this.getSeckillGoods(skuId));

        //  orderRecode这个实体类记录了秒杀的全部信息！
        String seckillOrderKey = RedisConst.SECKILL_ORDERS; // seckill:orders
        //  使用hash 数据类型 hset key=seckill:orders  field=userId value=orderRecode;
        redisTemplate.boundHashOps(seckillOrderKey).put(userId,orderRecode);

        //  减库存！mysql - redis！
        this.updateStockCount(skuId);
    }

    @Override
    public Result checkOrder(Long skuId, String userId) {
        /*
        1.  判断用户是否在缓存中存在
        2.  判断用户是否抢单成功  {再判断用户是否已经预下单}
        3.  判断用户是否下过订单
        4.  判断状态位
         */
        String seckillKey = RedisConst.SECKILL_USER + userId;
        Boolean flag = redisTemplate.hasKey(seckillKey);
        //  判断缓存中是否有用户存在
        if (flag){
            //  用户再缓存中存在！再判断用户是否已经预下单
            //  hset key = seckillOrderKey field = userId  value = orderRecode;
            String seckillOrderKey = RedisConst.SECKILL_ORDERS;
            Boolean res = redisTemplate.boundHashOps(seckillOrderKey).hasKey(userId);
            //  判断用户是否预下单成功
            if (res){
                //  表示预下单成功！抢购成功！去下单！
                OrderRecode orderRecode = (OrderRecode) redisTemplate.boundHashOps(seckillOrderKey).get(userId);
                return Result.build(orderRecode, ResultCodeEnum.SECKILL_SUCCESS);
            }
        }

        //  判断用户是否下过订单
        //  预下单{数据存储 hset key = seckillOrderKey field = userId  value = orderRecode;} 单纯只存缓存！
        //  真正下过订单了，在点击提交订单时的操作 ！那么需要在缓存中存储一份！同时在数据库中也有存储！
        //  缓存如何存在！ hset key = RedisConst.SECKILL_ORDERS_USERS  field =userId value = orderId;
        Boolean result = redisTemplate.boundHashOps(RedisConst.SECKILL_ORDERS_USERS).hasKey(userId);
        //  判断result
        if (result){
            //  表示用户已经提交过订单！
            //  hget key field
            String orderId = (String) redisTemplate.boundHashOps(RedisConst.SECKILL_ORDERS_USERS).get(userId);
            return Result.build(orderId, ResultCodeEnum.SECKILL_ORDER_SUCCESS);
        }
        //  校验状态位：
        String status = (String) CacheHelper.get(skuId.toString());
        if ("0".equals(status) || StringUtils.isEmpty(status)){
            //  已售罄！
            return Result.build(null,ResultCodeEnum.SECKILL_FAIL);
        }
        //  返回数据！
        return Result.build(null,ResultCodeEnum.SECKILL_RUN);
    }

    //  表示更新mysql -- redis 的库存数据！
    private void updateStockCount(Long skuId) {
        //  获取到存储库存剩余数！
        //  key = seckill:stock:46
        String stockKey = RedisConst.SECKILL_STOCK_PREFIX + skuId;
        //  redisTemplate.opsForList().leftPush(key,seckillGoods.getSkuId());
        Long count = redisTemplate.boundListOps(stockKey).size();
        //  减少库存数！方式一减少压力!
        if (count%2==0){
            //  开始更新数据！
            SeckillGoods seckillGoods = this.getSeckillGoods(skuId);
            //  赋值剩余库存数！
            seckillGoods.setStockCount(count.intValue());
            //  更新的数据库！
            seckillGoodsMapper.updateById(seckillGoods);
            //  更新缓存！
            redisTemplate.boundHashOps(RedisConst.SECKILL_GOODS).put(seckillGoods.getSkuId().toString(),seckillGoods);
        }
        //  方式二：可以采用异步方式！
    }
}
