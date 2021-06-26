package com.atguigu.gmall.mq.receiver;

import com.atguigu.gmall.mq.config.DelayedMqConfig;
import com.rabbitmq.client.Channel;
import lombok.SneakyThrows;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import java.text.SimpleDateFormat;
import java.util.Date;

@Component
public class DelayReceiver {

    //  监听消息
    @SneakyThrows
    @RabbitListener (queues = DelayedMqConfig.queue_delay_1)
    public void getMessage(String msg, Message message, Channel channel){
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        System.out.println("接收消息的时间:\t "+sdf.format(new Date())+"\t"+msg);
        //  手动确认
        channel.basicAck(message.getMessageProperties().getDeliveryTag(),false);
    }
}
