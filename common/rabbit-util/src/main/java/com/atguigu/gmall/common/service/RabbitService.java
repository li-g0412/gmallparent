package com.atguigu.gmall.common.service;

import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class RabbitService {
    @Autowired
    private RabbitTemplate rabbitTemplate;

    //封装发送消息的方法
    public boolean sengMessage(String exchange, String routingKey, Object message){
        //发送消息
        rabbitTemplate.convertAndSend(exchange,routingKey,message);
        return true;
    }
}
