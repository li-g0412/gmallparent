package com.atguigu.gmall.order.service;

import com.atguigu.gmall.model.order.OrderInfo;

public interface OrderService {

    //  保存订单！
    Long saveOrder(OrderInfo orderInfo);

    //生成流水号
    String getTradeNo(String userId);
    //比较流水号
    Boolean checkTradeNo(String tradeNo,Long userId);

    void deleteTradeNo(Long userId);
}
