package com.atguigu.gmall.list.service;

import com.atguigu.gmall.model.list.SearchParam;
import com.atguigu.gmall.model.list.SearchResponseVo;

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

    /**
     * 用户检索数据接口
     * @param searchParam
     * @return
     */
    SearchResponseVo search(SearchParam searchParam);

}
