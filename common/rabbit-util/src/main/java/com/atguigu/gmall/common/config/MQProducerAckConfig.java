package com.atguigu.gmall.common.config;

import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.connection.CorrelationData;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;

@Component
public class MQProducerAckConfig implements RabbitTemplate.ConfirmCallback ,RabbitTemplate.ReturnCallback {

    //  要想让代码走confirm 和 returnedMessage 则需要让 rabbitTemplate 跟这个类有关系！
    @Autowired
    private RabbitTemplate rabbitTemplate;

    //  初始化配置
    @PostConstruct
    public void init(){
        rabbitTemplate.setConfirmCallback(this);
        rabbitTemplate.setReturnCallback(this);
    }

    /**
     * 消息的确认
     * @param correlationData 可以承载消息的主体
     * @param ack   是否将消息发送到交换机上
     * @param cause 原因
     */
    @Override
    public void confirm(CorrelationData correlationData, boolean ack, String cause) {
        if (ack){
            System.out.println("发送成功!");
        }else {
            System.out.println("发送失败！");
        }
    }

    /**
     * 表示消息是否正确发送到队列中！ 当消息没有正确走到队列的时候走这个方法！
     * @param message   消息主体
     * @param replyCode 应答码
     * @param replyText 原因
     * @param exchange  交换机
     * @param routingKey  路由键
     */
    @Override
    public void returnedMessage(Message message, int replyCode, String replyText, String exchange, String routingKey) {
        //  判断：if（ exchange == xxx  routingKey == xxx  ） {不管！ } else { 需要记录日志处理！ }
        System.out.println("消息主体: " + new String(message.getBody()));
        System.out.println("应答码: " + replyCode);
        System.out.println("描述：" + replyText);
        System.out.println("消息使用的交换器 exchange : " + exchange);
        System.out.println("消息使用的路由键 routing : " + routingKey);
    }
}
