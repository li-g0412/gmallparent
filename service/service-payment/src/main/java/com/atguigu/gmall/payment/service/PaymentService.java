package com.atguigu.gmall.payment.service;

import com.atguigu.gmall.model.order.OrderInfo;

public interface PaymentService {

    //  保存交易记录！
    void savePaymentInfo(OrderInfo orderInfo,String paymentType);


}
