package com.atguigu.gmall.payment.service;

public interface AlipayService {

    //  参数返回值！
    String createaliPay(Long orderId);

    //  退款接口
    Boolean refund(Long orderId);

    /***
     * 关闭交易
     * @param orderId
     * @return
     */
    Boolean closePay(Long orderId);

    /**
     * 根据订单查询是否支付成功！
     * @param orderId
     * @return
     */
    Boolean checkPayment(Long orderId);


}
