package com.atguigu.gmall.mq.controller;

import com.atguigu.gmall.common.result.Result;
import com.atguigu.gmall.common.service.RabbitService;
import com.atguigu.gmall.mq.config.DeadLetterMqConfig;
import com.atguigu.gmall.mq.config.DelayedMqConfig;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.text.SimpleDateFormat;
import java.util.Date;

@RestController
@RequestMapping("mq")
public class MqController {

    @Autowired
    private RabbitService rabbitService;

    @Autowired
    private RabbitTemplate rabbitTemplate;

    //  封装一个发送消息的控制器！
    @GetMapping("sendConfirm")
    public Result sendConfirm(){
        //  需要将交换机，路由键，消息主体！
        rabbitService.sendMessage("exchange.confirm", "routing.confirm666","来人了，开始接客吧!");
        //  默认返回OK
        return Result.ok();
    }

    //  发送一个基于死信队列的消息！
    @GetMapping("sendDeadLettle")
    public Result sendDeadLettle(){
        //  在这可以声明一个时间，获取到发送消息的时间！
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        //  发送消息！
        rabbitService.sendMessage(DeadLetterMqConfig.exchange_dead,DeadLetterMqConfig.routing_dead_1,"发送成功!");
        System.out.println("发送时间：\t "+sdf.format(new Date()));
        //  默认返回OK
        return Result.ok();
    }

    //  发送一个基于插件的延迟消息！
    @GetMapping("sendDelay")
    public Result sendDelay(){
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

        //  rabbitService.sendDelayMessage(DelayedMqConfig.exchange_delay,DelayedMqConfig.routing_delay,"来来来,都来,门口集合!",10);
        //  rabbitService.sendMessage(DeadLetterMqConfig.exchange_dead,DeadLetterMqConfig.routing_dead_1,"发送成功!");
        rabbitTemplate.convertAndSend(DelayedMqConfig.exchange_delay,DelayedMqConfig.routing_delay,"来来来,都来,门口集合!",(message)->{
            //  设置配置 现在设置延迟10秒钟
            message.getMessageProperties().setDelay(10000);
            System.out.println("发送消息时间：\t "+sdf.format(new Date()));
            return message;
        });
        //        正常写法
//        rabbitTemplate.convertAndSend(DelayedMqConfig.exchange_delay, DelayedMqConfig.routing_delay, "来来来,都来,门口集合!", new MessagePostProcessor() {
//            @Override
//            public Message postProcessMessage(Message message) throws AmqpException {
//                message.getMessageProperties().setDelay(10000);
//                System.out.println("发送消息时间：\t "+sdf.format(new Date()));
//                return message;
//            }
//        });

        System.out.println("发送时间：\t "+sdf.format(new Date()));
        //  返回
        return Result.ok();
    }

}
