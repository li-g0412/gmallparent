package com.atguigu.gmall.payment.service;

public interface AlipayService {

    //  参数返回值！
    String createaliPay(Long orderId);

    //  退款接口
    Boolean refund(Long orderId);
}
