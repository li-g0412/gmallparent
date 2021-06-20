package com.atguigu.gmall.cart.service.impl;

import com.atguigu.gmall.cart.mapper.CartInfoMapper;
import com.atguigu.gmall.cart.service.CartService;
import com.atguigu.gmall.common.constant.RedisConst;
import com.atguigu.gmall.model.cart.CartInfo;
import com.atguigu.gmall.model.product.SkuInfo;
import com.atguigu.gmall.product.client.ProductFeignClient;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;

import java.sql.Timestamp;
import java.util.Date;

public class CartServiceImpl implements CartService {

    @Autowired
    private CartInfoMapper cartInfoMapper;
    @Autowired
    private ProductFeignClient productFeignClient;
    @Autowired
    private RedisTemplate redisTemplate;

    @Override
    public void addToCart(Long skuId, String userId, Integer skuNum) {
        /**
         * 1.查询购物车李是否有当前商品
         * true:直接数据相加,给实时价格赋值
         * false:直接插入数据库
         * 2.无论有没有商品都将这个商品添加到缓存
         */
        QueryWrapper<CartInfo> cartInfoQueryWrapper = new QueryWrapper<>();
        cartInfoQueryWrapper.eq("user_id", userId);
        cartInfoQueryWrapper.eq("sku_id", skuId);
        CartInfo cartInfoExist = cartInfoMapper.selectOne(cartInfoQueryWrapper);
        //  当前购物车中有该商品
        if (cartInfoExist != null) {
            //  数量相加
            cartInfoExist.setSkuNum(cartInfoExist.getSkuNum()+skuNum);
            //查询一下实时价格
            cartInfoExist.setSkuPrice(productFeignClient.getSkuPrice(skuId));
            //如果勾选状态发生变化的时候{没有选中}
            cartInfoExist.setIsChecked(1);
            //更新需要覆盖
            cartInfoExist.setUpdateTime(new Timestamp(new Date().getTime()));
            //通过id更新数据
            cartInfoMapper.updateById(cartInfoExist);
//            cartInfoMapper.update(cartInfoExist,cartInfoQueryWrapper);
            //使用缓存，必须考虑的问题
        }else {
            //当前商品在购物车中不存在
            CartInfo cartInfo = new CartInfo();
            //给cartInfo赋值
            SkuInfo skuInfo = productFeignClient.getSkuInfo(skuId);
            cartInfo.setUserId(userId);
            cartInfo.setSkuId(skuId);
            //  在初始化的时候，添加购物车的价格 = skuInfo.price
            cartInfo.setCartPrice(skuInfo.getPrice());
            //  数据库不存在的，购物车的价格 = skuInfo.price
            cartInfo.setSkuPrice(skuInfo.getPrice());
            cartInfo.setSkuNum(skuNum);
            cartInfo.setImgUrl(skuInfo.getSkuDefaultImg());
            cartInfo.setSkuName(skuInfo.getSkuName());
            cartInfo.setCreateTime(new Timestamp(new Date().getTime()));
            cartInfo.setUpdateTime(new Timestamp(new Date().getTime()));
            //  执行数据库操作
            cartInfoMapper.insert(cartInfo);
            cartInfoExist = cartInfo;
        }
        //使用缓存，必须考虑的问题
        String cartKey = getCarKey(userId);
        redisTemplate.opsForHash().put(cartKey,skuId.toString(),cartInfoExist);
//        redisTemplate.boundHashOps(cartKey).put(skuId.toString(),cartInfoExist);
    }
    private String getCarKey(String userId){
        return RedisConst.USER_KEY_PREFIX+userId+RedisConst.USER_CART_KEY_SUFFIX;
    }
}
