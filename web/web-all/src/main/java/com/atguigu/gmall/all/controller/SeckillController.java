package com.atguigu.gmall.all.controller;

import com.atguigu.gmall.activity.client.ActivityFeignClient;
import com.atguigu.gmall.common.result.Result;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import javax.servlet.http.HttpServletRequest;

@Controller
public class SeckillController {

    @Autowired
    private ActivityFeignClient activityFeignClient;

    //编写秒杀控制器
    @GetMapping("seckill.html")
    public String seckillIndex(Model model){
        Result result = activityFeignClient.findAll();
        model.addAttribute("list", result.getData());
        //返回页面
        return "seckill/index";
    }

    @GetMapping("seckill/{skuId}.html")
    public String seckillItem(@PathVariable Long skuId,Model model){
        Result result = activityFeignClient.getSeckillGoods(skuId);
        model.addAttribute("item",result.getData());
        return "seckill/item";
    }

    @GetMapping("seckill/queue.html")
    public String seckillQueue(HttpServletRequest request){
        String skuId = request.getParameter("skuId");
        String skuIdStr = request.getParameter("skuIdStr");
        request.setAttribute("skuId", skuId);
        request.setAttribute("skuIdStr", skuIdStr);
        //返回视图页面名称
        return "seckill/queue";
    }
}
