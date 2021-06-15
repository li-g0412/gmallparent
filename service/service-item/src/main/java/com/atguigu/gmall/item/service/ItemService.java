package com.atguigu.gmall.item.service;

import java.util.Map;

/**
 * @author atguigu-mqx
 */
public interface ItemService {

    /**
     * 数据汇总 明确返回值，以及参数列表！
     * @param skuId
     * @return
     */
    Map<String,Object> getItem(Long skuId);

}
