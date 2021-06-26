package com.atguigu.gmall.common.service;

import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class RabbitService {

    @Autowired
    private RabbitTemplate rabbitTemplate;

    //  封装发送消息的方法！
    public boolean sendMessage(String exchange,String routingKey,Object message){
        //  发送消息方法
        rabbitTemplate.convertAndSend(exchange,routingKey,message);

        //  默认返回true
        return true;
    }

    /**
     * 封装一个发送延迟消息的方法！
     * @param exchange  交换机
     * @param routingKey    路由键
     * @param object    消息主体
     * @param delayTime 过期时间 单位毫秒！
     * @return
     */
    public boolean sendDelayMessage(String exchange,String routingKey,Object object, int delayTime){
        //  发送延迟消息的方法！
        rabbitTemplate.convertAndSend(exchange,routingKey,object ,(message)->{
            //  设置配置 现在设置延迟10秒钟
            message.getMessageProperties().setDelay(delayTime*1000);
            return message;
        });
        return true;
    }
}
