package com.atguigu.gmall.order.config;

import com.atguigu.gmall.common.constant.MqConst;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.CustomExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.HashMap;

@Configuration
public class OrderCanelMqConfig {

    //  编写交换机，队列，设置绑定关系！
    @Bean
    public CustomExchange orderDelayExchange(){
        HashMap<String, Object> map = new HashMap<>();
        //  对map 进行参数设置
        map.put("x-delayed-type","direct");
        //  返回对象
        return new CustomExchange(MqConst.EXCHANGE_DIRECT_ORDER_CANCEL,"x-delayed-message",true,false,map);
    }

    @Bean
    public Queue orderDelayQueue(){
        return new Queue(MqConst.QUEUE_ORDER_CANCEL,true,false,false);
    }

    //  设置绑定关系!
    @Bean
    public Binding delayBinding(){
        //  返回对象
        return BindingBuilder.bind(orderDelayQueue()).to(orderDelayExchange()).with(MqConst.ROUTING_ORDER_CANCEL).noargs();
    }


}
