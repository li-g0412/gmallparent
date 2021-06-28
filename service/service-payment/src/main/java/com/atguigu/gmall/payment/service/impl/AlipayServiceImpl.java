package com.atguigu.gmall.payment.service.impl;

import com.alibaba.fastjson.JSON;
import com.alipay.api.AlipayApiException;
import com.alipay.api.AlipayClient;
import com.alipay.api.request.AlipayTradePagePayRequest;
import com.atguigu.gmall.model.enums.PaymentType;
import com.atguigu.gmall.model.order.OrderInfo;
import com.atguigu.gmall.order.client.OrderFeignClient;
import com.atguigu.gmall.payment.config.AlipayConfig;
import com.atguigu.gmall.payment.service.AlipayService;
import com.atguigu.gmall.payment.service.PaymentService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.HashMap;

@Service
public class AlipayServiceImpl implements AlipayService {

    @Autowired
    private AlipayClient alipayClient;

    @Autowired
    private OrderFeignClient orderFeignClient;

    @Autowired
    private PaymentService paymentService;


    @Override
    public String createaliPay(Long orderId) {
        //  根据订单Id 获取orderInfo
        OrderInfo orderInfo = orderFeignClient.getOrderInfo(orderId);
        if ("PAID".equals(orderInfo.getOrderStatus()) || "CLOSED".equals(orderInfo.getOrderStatus())){
            return "该订单已经完成或已经关闭!";
        }
        //  调用保存交易记录方法！
        paymentService.savePaymentInfo(orderInfo, PaymentType.ALIPAY.name());

        String form = "";
        //  创建 AlipayClient 对象！ AlipayClient 这个对象注入到 spring 容器中！
        //  AlipayClient alipayClient =  new DefaultAlipayClient( "https://openapi.alipay.com/gateway.do" , APP_ID, APP_PRIVATE_KEY, FORMAT, CHARSET, ALIPAY_PUBLIC_KEY, SIGN_TYPE);  //获得初始化的AlipayClient
        AlipayTradePagePayRequest alipayRequest =  new  AlipayTradePagePayRequest(); //创建API对应的request
        //  同步回调 http://api.gmall.com/api/payment/alipay/callback/return
        alipayRequest.setReturnUrl(AlipayConfig.return_payment_url);
        //  异步回调
        alipayRequest.setNotifyUrl( "http://domain.com/CallBack/notify_url.jsp" ); //在公共参数中设置回跳和通知地址
        //  封装业务参数
        HashMap<String, Object> map = new HashMap<>();
        //  第三方业务编号！
        map.put("out_trade_no",orderInfo.getOutTradeNo());
        map.put("product_code","FAST_INSTANT_TRADE_PAY");
        map.put("total_amount","0.01");
        map.put("subject",orderInfo.getTradeBody());
        //  设置二维码过期时间
        map.put("timeout_express","10m");
        alipayRequest.setBizContent(JSON.toJSONString(map));
        try  {
            form = alipayClient.pageExecute(alipayRequest).getBody();  //调用SDK生成表单
        }  catch  (AlipayApiException e) {
            e.printStackTrace();
        }
        return form;
    }
}
