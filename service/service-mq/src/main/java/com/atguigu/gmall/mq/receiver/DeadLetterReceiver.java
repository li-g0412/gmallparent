package com.atguigu.gmall.mq.receiver;

import com.atguigu.gmall.mq.config.DeadLetterMqConfig;
import com.rabbitmq.client.Channel;
import lombok.SneakyThrows;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import java.text.SimpleDateFormat;
import java.util.Date;

@Component
public class DeadLetterReceiver {

    //  设置监听
        //    @RabbitListener(bindings = @QueueBinding(
        //            value = @Queue(value = "",durable = "true",autoDelete = "false"),
        //            exchange = @Exchange(value = ""),
        //            key = {}
        //    ))
        //    public void getMessage(){
        //
        //    }
    @SneakyThrows
    @RabbitListener(queues = DeadLetterMqConfig.queue_dead_2)
    public void  getMssage(String msg, Message message, Channel channel){

        //  声明时间
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

        System.out.println("接收到的消息：\t"+msg+":\t 接收的时间"+sdf.format(new Date()));
        //  手动确认！
        channel.basicAck(message.getMessageProperties().getDeliveryTag(),false);
    }

}
