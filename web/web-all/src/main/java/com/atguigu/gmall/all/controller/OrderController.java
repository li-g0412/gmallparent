package com.atguigu.gmall.all.controller;

import com.atguigu.gmall.common.result.Result;
import com.atguigu.gmall.order.client.OrderFeignClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;

import javax.servlet.http.HttpServletRequest;
import java.util.Map;

@Controller
public class OrderController {

    //  web-all 远程调用！
    @Autowired
    private OrderFeignClient orderFeignClient;

    //  http://order.gmall.com/trade.html
    @RequestMapping("trade.html")
    public String trade( HttpServletRequest request, Model model){
        //  远程调用orderFeignClient
        Result<Map<String, Object>> result = orderFeignClient.trade();

        //  ${tradeNo}
        //  model.addAttribute("tradeNo",orderFeignClient.getTradeNo());
        //  返回页面视图名称  页面需要 userAddressList ， detailArrayList ，totalNum  ，totalAmount
        //        userAddressList = orderFeignClient.xxx();
        //        detailArrayList = orderFeignClient.xxx();
        //        detailArrayList = orderFeignClient.xxx();
        //        detailArrayList = orderFeignClient.xxx();
        //        request.setAttribute("","");
        //        request.setAttribute("","");
        //        request.setAttribute("","");
        //        request.setAttribute("","");
        //        Map map = new HashMap();
        //        map.put("userAddressList",userAddressList);
        //        Result<Map> result = orderFeignClient.xxx();
        model.addAllAttributes(result.getData());
        return "order/trade";
    }
}
