package com.atguigu.gmall.cart.service.impl;

import com.atguigu.gmall.cart.mapper.CartInfoMapper;
import com.atguigu.gmall.cart.service.CartService;
import com.atguigu.gmall.common.constant.RedisConst;
import com.atguigu.gmall.common.util.DateUtil;
import com.atguigu.gmall.model.cart.CartInfo;
import com.atguigu.gmall.model.product.SkuInfo;
import com.atguigu.gmall.product.client.ProductFeignClient;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.sql.Timestamp;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Service
public class CartServiceImpl implements CartService {

    @Autowired
    private CartInfoMapper cartInfoMapper;

    @Autowired
    private ProductFeignClient productFeignClient;

    @Autowired
    private RedisTemplate redisTemplate;


    @Override
    @Transactional(rollbackFor = Exception.class)
    public void addToCart(Long skuId, String userId, Integer skuNum) {
        /*
        1.  查询购物车中是否已经有当前的商品
            true:
                直接将数量相加，给实时价格赋值...
            false:
                直接插入 insert
         2.  无论是有商品，还是没有商品都将这个商品添加到缓存Redis！
         */
        //  获取缓存key
        String cartKey = getCartKey(userId);

        //  判断购物车的key 是否存在！
        if (!redisTemplate.hasKey(cartKey)){
            //  将数据库中的数据查询并放入缓存！
            this.loadCartCache(userId);
        }

        //  select * from cart_info where user_id = ? and sku_id = ?
        //        QueryWrapper<CartInfo> cartInfoQueryWrapper = new QueryWrapper<>();
        //        cartInfoQueryWrapper.eq("user_id",userId);
        //        cartInfoQueryWrapper.eq("sku_id",skuId);
        //  CartInfo cartInfoExist = cartInfoMapper.selectOne(cartInfoQueryWrapper);

        //  hget key field
        CartInfo cartInfoExist = (CartInfo) redisTemplate.opsForHash().get(cartKey, skuId.toString());
        //  判断购物车中是否有当前商品
        if (cartInfoExist!=null){
            //  说明购物车中已经有该商品了！
            cartInfoExist.setSkuNum(cartInfoExist.getSkuNum()+skuNum);
            //  查询一下实时价格 skuInfo.price
            cartInfoExist.setSkuPrice(productFeignClient.getSkuPrice(skuId));
            //  如果勾选状态发生变化的时候{没有选中}，第二次添加的时候要覆盖！
            //            if (cartInfoExist.getIsChecked().intValue()==0){
            //                cartInfoExist.setIsChecked(1);
            //            }
            cartInfoExist.setIsChecked(1);
            //  更新需要覆盖了
            cartInfoExist.setUpdateTime(new Timestamp(new Date().getTime()));
            //  更新数据：通过Id 更新！
            cartInfoMapper.updateById(cartInfoExist);
            //  第一个参数：表示要更新的内容，第二个是更新的条件！
            //  cartInfoMapper.update(cartInfoExist,cartInfoQueryWrapper);
            //  int i = 1/0;
            //  操作缓存！cartInfoExist
        }else {
            //  当前商品在购物车列表中不存在！
            CartInfo cartInfo = new CartInfo();
            //  给cartInfo 赋值！
            SkuInfo skuInfo = productFeignClient.getSkuInfo(skuId);

            //  给表的字段赋值！
            cartInfo.setUserId(userId);
            cartInfo.setSkuId(skuId);
            cartInfo.setCartPrice(skuInfo.getPrice());
            cartInfo.setSkuNum(skuNum);
            cartInfo.setImgUrl(skuInfo.getSkuDefaultImg());
            cartInfo.setSkuName(skuInfo.getSkuName());
            cartInfo.setCreateTime(new Timestamp(new Date().getTime()));
            cartInfo.setUpdateTime(new Timestamp(new Date().getTime()));
            //  非表的字段！
            cartInfo.setSkuPrice(productFeignClient.getSkuPrice(skuId));
            //  直接插入数据库！
            cartInfoMapper.insert(cartInfo);

            //  操作缓存！ cartInfo
            //  废物利用！
            cartInfoExist = cartInfo;

        }
        //  使用缓存，必须考虑的问题！ 数据类型--hash ，key！
        //  hset key field value; hget key field  key = 唯一！ field = skuId value = cartInfo;
        redisTemplate.opsForHash().put(cartKey,skuId.toString(),cartInfoExist);

        //  redisTemplate.boundHashOps(cartKey).put(skuId.toString(),cartInfoExist);
        //  可以给购物车设置过期时间！
        setCartKeyExpire(cartKey);
    }

    @Override
    public List<CartInfo> getCartList(String userId, String userTempId) {
        //  声明一个集合对象
        List<CartInfo> cartInfoList = new ArrayList<>();
        //  判断
        if(StringUtils.isEmpty(userId)){
            //  查看临时用户Id 购物车列表
            cartInfoList = getCartList(userTempId);
        }

        //  查看登录的用户Id
        if (!StringUtils.isEmpty(userId)){
            //  合并的购物车业务逻辑！
            //  获取未登录购物车数据： userTempId 什么时候产生的，点击添加购物车时，没有临时用户Id，同时没有userId ，产生 放入cookie 中！
            if (StringUtils.isEmpty(userTempId)){
                cartInfoList = getCartList(userId);
                return cartInfoList;
            }
            List<CartInfo> noLoginCartList = getCartList(userTempId);
            //  判断 未登录状态下有购物车
            if (!CollectionUtils.isEmpty(noLoginCartList)){
                //  合并购物车方法
                cartInfoList = this.mergeToCartList(noLoginCartList,userId);
                //  删除临时购物车数据集合
                this.deleteCartList(userTempId);
            }else {
                //  只有登录的购物车数据
                cartInfoList = getCartList(userId);
            }
        }
        //  返回数据
        return cartInfoList;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void checkCart(String userId, Integer isChecked, Long skuId) {

        //  mysql , redis
        CartInfo cartInfoUpd = new CartInfo();
        cartInfoUpd.setIsChecked(isChecked);
        UpdateWrapper<CartInfo> cartInfoUpdateWrapper = new UpdateWrapper<>();
        cartInfoUpdateWrapper.eq("sku_id",skuId);
        cartInfoUpdateWrapper.eq("user_id",userId);
        cartInfoMapper.update(cartInfoUpd,cartInfoUpdateWrapper);

        //  获取到key
        String cartKey = this.getCartKey(userId);
        // hget key field
        CartInfo cartInfo = (CartInfo) redisTemplate.opsForHash().get(cartKey, skuId.toString());
        if (cartInfo!=null){
            //  用户的选中状态赋值给cartInfo
            cartInfo.setIsChecked(isChecked);
            //  在写入缓存！ hset key field value;
            redisTemplate.opsForHash().put(cartKey,skuId.toString(),cartInfo);
        }

    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteCart(Long skuId, String userId) {
        //  mysql redis
        //  delete from cart_info where user_id = ? and sku_id = ?;
        cartInfoMapper.delete(new QueryWrapper<CartInfo>().eq("user_id",userId).eq("sku_id",skuId));
        //  删除redis！
        //  获取到key
        String cartKey = this.getCartKey(userId);
        //  判断购物车中是否有该商品！
        Boolean flag = redisTemplate.boundHashOps(cartKey).hasKey(skuId.toString());
        if (flag){
            redisTemplate.boundHashOps(cartKey).delete(skuId.toString());
        }

    }

    @Override
    public List<CartInfo> getCartCheckedList(String userId) {
        //  select * from cart_info where user_id = ?; 在缓存的购物车列中已经存在购物车集合！所以直接获取缓存！
        //  获取缓存的key
        String cartKey = this.getCartKey(userId);
        //  获取到所有数据hvals
        List<CartInfo> cartInfoList = redisTemplate.boundHashOps(cartKey).values();
        //  声明一个存储选中的购物车集合
        //  ArrayList<CartInfo> cartInfoCheckList = new ArrayList<>();
        //  需要判断选中状态为 1
        //        for (CartInfo cartInfo : cartInfoList) {
        //            if (cartInfo.getIsChecked().intValue()==1){
        //                cartInfoCheckList.add(cartInfo);
        //            }
        //        }
        List<CartInfo> cartInfoCheckList = cartInfoList.stream().filter((cartInfo) -> {
            return cartInfo.getIsChecked().intValue() == 1;
        }).collect(Collectors.toList());
        //  返回数据即可！
        return cartInfoCheckList;
    }

    /**
     * 合并购物车数据集合
     * @param noLoginCartList   未登录购物车集合
     * @param userId    通过userId 查询到登录的购物车集合
     * @return
     */
    @Transactional(rollbackFor = Exception.class)
    public List<CartInfo> mergeToCartList(List<CartInfo> noLoginCartList, String userId) {
        //  声明一个购物车集合对象
        List<CartInfo> cartInfoList = new ArrayList<>();
        /*
        合并条件skuId 相同
        demo:
            登录：
                37 1
                38 1
            未登录：
                37 1
                38 1
                39 1
            合并之后的数据
                37 2
                38 2
                39 1

          登录：

          未登录：
            38 1
            39 1
         */
        //  根据userId 获取登录购物车数据
        List<CartInfo> logInCartList = this.getCartList(userId);
        //  第一种方式：两层循环遍历找到相同的skuId !
        //  第二种方式：将list 集合转换为map 集合 map.put(key,value) ; key = skuId value = CartInfo!
        //  登录的购物车map 集合
        Map<Long, CartInfo> longCartInfoMap = logInCartList.stream().collect(Collectors.toMap(CartInfo::getSkuId, cartInfo -> cartInfo));
        //  循环遍历
        for (CartInfo noLoginCartInfo : noLoginCartList) {
            //  判断是否有包含关系！
            //  有相同的skuId   37,38
            if (longCartInfoMap.containsKey(noLoginCartInfo.getSkuId())){
                //  对购物车中的商品数量进行·相加
                CartInfo logInCartInfo = longCartInfoMap.get(noLoginCartInfo.getSkuId());
                logInCartInfo.setSkuNum(logInCartInfo.getSkuNum()+noLoginCartInfo.getSkuNum());
                //  修改更新时间
                logInCartInfo.setUpdateTime(new Timestamp(new Date().getTime()));
                //  细节处理： 选中状态处理
                if (noLoginCartInfo.getIsChecked().intValue()==1){
                    //  46 赋值！
                    //                        if (logInCartInfo.getIsChecked().intValue()!=1){
                    //                            logInCartInfo.setIsChecked(1);
                    //                        }
                    //  45，46 赋值
                    logInCartInfo.setIsChecked(1);
                }
                //  执行更新语句！
                cartInfoMapper.updateById(logInCartInfo);

            }else {
                //  没有相同的skuId  39
                //  细节：应该知道当前userId 是谁了！
                noLoginCartInfo.setUserId(userId);
                noLoginCartInfo.setCreateTime(new Timestamp(new Date().getTime()));
                noLoginCartInfo.setUpdateTime(new Timestamp(new Date().getTime()));
                cartInfoMapper.insert(noLoginCartInfo);
            }
        }
        //  汇总获取到 37,38,39
        cartInfoList = loadCartCache(userId);

        //  返回购物车集合
        return cartInfoList;
    }

    /**
     * 删除未登录购物车数据
     * @param userTempId
     */
    @Transactional(rollbackFor = Exception.class)
    public void deleteCartList(String userTempId) {
        //  删除购物车：mysql redis！
        //  delete from cart_info where user_id = ?
        cartInfoMapper.delete(new QueryWrapper<CartInfo>().eq("user_id",userTempId));
        //  获取到缓存的key
        String cartKey = this.getCartKey(userTempId);
        //  判断是否存在！
        Boolean flag = redisTemplate.hasKey(cartKey);
        if (flag){
            redisTemplate.delete(cartKey);
        }


    }

    /**
     * 根据userId 查询购物车列表！
     * @param userId
     * @return
     */
    private List<CartInfo> getCartList(String userId) {

        //  声明一个集合对象
        List<CartInfo> cartInfoList = new ArrayList<>();
        /*
            第一件：查询缓存 ，缓存中没有数据，则查询数据，查询之后的数据再次放入缓存！
         */
        //  判断临时用户Id
        if (StringUtils.isEmpty(userId)){
            return cartInfoList;
        }

        //  获取缓存的key ：
        String cartKey = this.getCartKey(userId);
        //  使用hash 数据类型 ，如何获取到数据！ hset key field value  hget key field !
        //  hvals key
        //  List<CartInfo> cartInfoList = redisTemplate.opsForHash().values(cartKey);
        cartInfoList = redisTemplate.boundHashOps(cartKey).values();
        //  判断一下
        if(!CollectionUtils.isEmpty(cartInfoList)){
            //  展示购物车列表的时候应该有顺序！ 京东：按照更新时间！ 苏宁：创建时间！
            cartInfoList.sort((o1,o2)->{
                //  使用时间进行比较
                return DateUtil.truncatedCompareTo(o2.getUpdateTime(),o1.getUpdateTime(), Calendar.SECOND);
            });
        }else {
            //  缓存中的数据是空的，那么就应该走数据库！
            cartInfoList = this.loadCartCache(userId);
        }
        //  返回购物车数据集合
        return cartInfoList;
    }

    /**
     * 缓存没有数据的时候应该查询数据库，并将数据放入缓存！
     * @param userId
     * @return
     */
    private List<CartInfo> loadCartCache(String userId) {
        //  创建集合对象
        List<CartInfo> cartInfoList = new ArrayList<>();

        //  如何查询数据？ select * from cart_info where user_id =1 order by update_time desc;
        QueryWrapper<CartInfo> cartInfoQueryWrapper = new QueryWrapper<>();
        cartInfoQueryWrapper.eq("user_id",userId);
        cartInfoQueryWrapper.orderByDesc("update_time");
        cartInfoList = cartInfoMapper.selectList(cartInfoQueryWrapper);
        //  判断一下集合
        if (CollectionUtils.isEmpty(cartInfoList)){
            return cartInfoList;
        }
        //  数据库中有购物车列表应该将数据放入缓存！
        //  获取到缓存的key
        String cartKey = this.getCartKey(userId);
        HashMap<String, Object> map = new HashMap<>();

        //  循环遍历
        for (CartInfo cartInfo : cartInfoList) {
            //  hset key field value;
            //  redisTemplate.opsForHash().put(cartKey,cartInfo.getSkuId().toString(),cartInfo);
            //  hmset key field value field value ;
            //  给当前购物车对象赋值实时价格！
            cartInfo.setSkuPrice(productFeignClient.getSkuPrice(cartInfo.getSkuId()));
            map.put(cartInfo.getSkuId().toString(),cartInfo);
        }
        //  一次放入数据即可！
        redisTemplate.opsForHash().putAll(cartKey,map);

        //  可以给过期时间！
        this.setCartKeyExpire(cartKey);

        return cartInfoList;
    }

    //  设置key 的过期时间！
    private void setCartKeyExpire(String cartKey) {
        redisTemplate.expire(cartKey, RedisConst.USER_CART_EXPIRE, TimeUnit.SECONDS);
    }

    //  获取购物车的key
    private String getCartKey(String userId) {
        return RedisConst.USER_KEY_PREFIX+userId+RedisConst.USER_CART_KEY_SUFFIX;
    }
}
