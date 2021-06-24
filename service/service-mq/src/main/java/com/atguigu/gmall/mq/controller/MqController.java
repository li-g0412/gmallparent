package com.atguigu.gmall.mq.controller;

import com.atguigu.gmall.common.result.Result;
import com.atguigu.gmall.common.service.RabbitService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("mq")
public class MqController {

    @Autowired
    private RabbitService rabbitService;

    //封装一个发送消息的控制器
    @GetMapping("sendConfirm")
    public Result sendConfirm() {
        rabbitService.sengMessage("exchange.confirm", "routing.confirm","来人了！");
        return Result.ok();
    }
}
