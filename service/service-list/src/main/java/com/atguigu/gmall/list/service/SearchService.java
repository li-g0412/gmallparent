package com.atguigu.gmall.list.service;

/**
 * @author atguigu-mqx
 */
public interface SearchService {

    //  上架skuId
    void upperGoods(Long skuId);
    //  下架skuId
    void lowerGoods(Long skuId);

    /**
     * 更新热点
     * @param skuId
     */
    void incrHotScore(Long skuId);

}
