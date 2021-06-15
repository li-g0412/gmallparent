package com.atguigu.gmall.all.controller;

import com.atguigu.gmall.common.result.Result;
import com.atguigu.gmall.item.client.ItemFeignClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;

import java.util.Map;

/**
 * @author atguigu-mqx
 */
@Controller
public class ItemController {

    @Autowired
    private ItemFeignClient itemFeignClient;

    //  这个映射路径应该是什么?    https://item.jd.com/100008348532.html
    @RequestMapping("{skuId}.html")
    public String item(@PathVariable Long skuId, Model model){
        //  获取到商品详情微服务数据
        Result<Map> result = itemFeignClient.getItem(skuId);

        //  result.getData(); 获取的就是map 集合！
        model.addAllAttributes(result.getData());
        //  返回视图页面
        return "item/index";
    }

}
