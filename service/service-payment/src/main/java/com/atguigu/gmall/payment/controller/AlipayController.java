package com.atguigu.gmall.payment.controller;

import com.atguigu.gmall.payment.config.AlipayConfig;
import com.atguigu.gmall.payment.service.AlipayService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

@Controller
@RequestMapping("/api/payment/alipay")
public class AlipayController {

    @Autowired
    private AlipayService alipayService;

    //  api/payment/alipay/submit/{orderId}
    @RequestMapping("submit/{orderId}")
    @ResponseBody
    public String payOrder(@PathVariable Long orderId){

        //  调用服务层方法
        String form = alipayService.createaliPay(orderId);

        return form;
    }

    //  设置一个回调 callback/return
    @GetMapping("callback/return")
    public String retrunCallback(){
        //  同步回调！ 返回一个给用户看的地址！
        //  redirect: "http://payment.gmall.com/pay/success.html" 支付成功的页面！跟web-all 有关系！
        //  重定向：
        return "redirect:"+ AlipayConfig.return_order_url;
    }

    @PostMapping("callback/notify")
    @ResponseBody
    public String callbackNotify(){
        boolean signVerified = false;

        return "success";
    }
}


