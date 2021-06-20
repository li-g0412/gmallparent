package com.atguigu.gmall.cart.service;

public interface CartService {
    //添加购物车接口数据
    void addToCart(Long skuId, String userId, Integer skuNum);
}
