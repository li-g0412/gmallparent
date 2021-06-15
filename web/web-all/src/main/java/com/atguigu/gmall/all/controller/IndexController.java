package com.atguigu.gmall.all.controller;

import com.atguigu.gmall.common.result.Result;
import com.atguigu.gmall.product.client.ProductFeignClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import java.io.FileWriter;
import java.io.IOException;

/**
 * @author atguigu-mqx
 */
@Controller
public class IndexController {

    @Autowired
    private ProductFeignClient productFeignClient;

    @Autowired
    private TemplateEngine templateEngine;

    //  http://www.gmall.com  或  http://www.gmall.com/index.html
    @GetMapping({"/","index.html"})
    public String index(Model model){
        Result result = productFeignClient.getBaseCategoryList();
        //  前端需要后台存储一个list
        model.addAttribute("list",result.getData());
        //  返回视图名称
        return "index/index";
    }

    //  生成静态页面，
    @GetMapping("createIndex")
    @ResponseBody
    public Result createIndex(){
        //  获取后台的数据
        Result result = productFeignClient.getBaseCategoryList();

        //  创建对象 设置页面显示的数据
        Context context = new Context();
        context.setVariable("list",result.getData());

        //  创建写对象
        FileWriter fileWriter = null;
        try {
            fileWriter = new FileWriter("D:\\index.html");
        } catch (IOException e) {
            e.printStackTrace();
        }

        templateEngine.process("index/index.html",context,fileWriter);
        //  默认返回值
        return Result.ok();
    }


}
