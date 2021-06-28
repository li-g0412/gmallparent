package com.atguigu.gmall.all.controller;

import com.atguigu.gmall.model.order.OrderInfo;
import com.atguigu.gmall.order.client.OrderFeignClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

import javax.servlet.http.HttpServletRequest;

@Controller
public class PaymentController {

    @Autowired
    private OrderFeignClient orderFeignClient;

    @GetMapping("pay.html")
    public String success(HttpServletRequest request){
        String orderId = request.getParameter("orderId");
        OrderInfo orderInfo = orderFeignClient.getOrderInfo(Long.parseLong(orderId));
        request.setAttribute("orderInfo",orderInfo);
        return "payment/pay";
    }

    //  http://payment.gmall.com/pay/success.html
    @GetMapping("pay/success.html")
    public String paySuccess(){
        //  支付成功之后的页面！
        return "payment/success";
    }
}
