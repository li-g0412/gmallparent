package com.atguigu.gmall.mq.config;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.HashMap;

@Configuration
public class DeadLetterMqConfig {

    //  定义写变量！
    public static final String exchange_dead = "exchange.dead";
    public static final String routing_dead_1 = "routing.dead.1";
    public static final String routing_dead_2 = "routing.dead.2";
    public static final String queue_dead_1 = "queue.dead.1";
    public static final String queue_dead_2 = "queue.dead.2";

    //  声明一个交换机，队列，绑定关系！
    @Bean
    public DirectExchange exchange(){
        //  创建并返回这个对象
        //  第一个交换机，第二个是否持久化，第三个是否自动删除 || 第四个参数是否有其他额外的配置！
        return new DirectExchange(exchange_dead,true,false);
    }

    //  声明队列1
    @Bean
    public Queue queue1(){
        //  设置消息的TTL
        HashMap<String, Object> map = new HashMap<>();
        // 默认毫秒！ 10秒！
        map.put("x-message-ttl",10000);
        map.put("x-dead-letter-exchange",exchange_dead);
        //  由路由键2绑定到队列2！
        map.put("x-dead-letter-routing-key",routing_dead_2);
        //  第一个参数,第二个参数是否持久化,第三个参数是否排外,第四个参数是否自动删除,第五个参数是否有额外设置！
        return new Queue(queue_dead_1,true,false,false,map);

    }

    //  声明队列2
    @Bean
    public Queue queue2(){
        //  返回
        return new Queue(queue_dead_2,true,false,false);
    }

    //  设置绑定关系！
    @Bean
    public Binding binding1(){
        //  返回当前对象 Binding
        //  new Binding();
        return BindingBuilder.bind(queue1()).to(exchange()).with(routing_dead_1);
    }

    @Bean
    public Binding binding2(){
        //  返回当前对象 Binding
        //  new Binding();
        return BindingBuilder.bind(queue2()).to(exchange()).with(routing_dead_2);
    }
}
