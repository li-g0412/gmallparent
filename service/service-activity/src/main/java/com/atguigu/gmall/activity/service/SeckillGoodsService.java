package com.atguigu.gmall.activity.service;

import com.atguigu.gmall.common.result.Result;
import com.atguigu.gmall.model.activity.SeckillGoods;

import java.util.List;

/**
 * @author atguigu-mqx
 */
public interface SeckillGoodsService {

    //  查询所有的秒杀商品！
    List<SeckillGoods> findAll();

    //  查询秒杀的详情页面!
    SeckillGoods getSeckillGoods(Long skuId);

    //  保存预下单数据
    void seckillOrder(Long skuId, String userId);

    //  检查秒杀状态
    Result checkOrder(Long skuId, String userId);

}
