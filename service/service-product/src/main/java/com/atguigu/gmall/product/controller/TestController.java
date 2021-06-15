package com.atguigu.gmall.product.controller;

import com.atguigu.gmall.common.result.Result;
import com.atguigu.gmall.product.service.TestService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * @author atguigu-mqx
 */
@RestController
@RequestMapping("admin/product/test")
public class TestController {


    @Autowired
    private TestService testService;

    //  自定义一个映射路径：
    @GetMapping("testLock")
    public Result testLock(){
        //  调用服务层方法
        testService.testLock();
        //  返回
        return Result.ok();

    }

    //  读锁
    @GetMapping("read")
    public Result read(){
        //  调用服务层方法
        String msg =  testService.readLock();
        //  返回数据
        return Result.ok(msg);
    }

    //  写锁
    @GetMapping("write")
    public Result write(){
        //  调用服务层方法
        String msg =  testService.writeLock();
        //  返回数据
        return Result.ok(msg);
    }


}
