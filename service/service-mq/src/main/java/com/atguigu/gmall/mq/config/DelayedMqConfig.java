package com.atguigu.gmall.mq.config;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.CustomExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.HashMap;

@Configuration
public class DelayedMqConfig {

    //  声明变量：
    public static final String exchange_delay = "exchange.delay";
    public static final String routing_delay = "routing.delay";
    public static final String queue_delay_1 = "queue.delay.1";

    //  需要制作队列，交换机，绑定关系！
    @Bean
    public Queue delayQueue(){
        //  基于插件的方式，不需要再队列中进行设置！
        //  直接返回
        return new Queue(queue_delay_1,true,false,false);
    }

    //  声明交换机
    @Bean
    public CustomExchange delayExchange(){
        HashMap<String, Object> map = new HashMap<>();
        //  对map 进行参数设置
        map.put("x-delayed-type","direct");
        //  返回对象
        return new CustomExchange(exchange_delay,"x-delayed-message",true,false,map);
    }

    //  设置绑定关系!
    @Bean
    public Binding delayBinding(){

        //  返回对象
        //  return BindingBuilder.bind(queue1()).to(exchange()).with(routing_dead_1);
        //  BindingBuilder.GenericArgumentsConfigurer with = BindingBuilder.bind(delayQueue()).to(delayExchange()).with(routing_delay);
        return BindingBuilder.bind(delayQueue()).to(delayExchange()).with(routing_delay).noargs();
    }

}
