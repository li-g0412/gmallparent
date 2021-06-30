package com.atguigu.gmall.list.receiver;

import com.atguigu.gmall.common.constant.MqConst;
import com.atguigu.gmall.list.service.SearchService;
import com.rabbitmq.client.Channel;
import lombok.SneakyThrows;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.Exchange;
import org.springframework.amqp.rabbit.annotation.Queue;
import org.springframework.amqp.rabbit.annotation.QueueBinding;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class ListReceiver {

    @Autowired
    private SearchService searchService;

    //  开启消息监听 监听商品上架！
    @SneakyThrows
    @RabbitListener(bindings = @QueueBinding(
            value = @Queue(value = MqConst.QUEUE_GOODS_UPPER,durable = "true",autoDelete = "false"),
            exchange = @Exchange(value = MqConst.EXCHANGE_DIRECT_GOODS),
            key = {MqConst.ROUTING_GOODS_UPPER}
    ))
    public void upperGoodsToEs(Long skuId, Message message, Channel channel){
        //  获取到skuId,并判断
        try {
            if (skuId!=null){
                //  则调用商品上架的方法！
                searchService.upperGoods(skuId);
            }
        } catch (Exception e) {
            //  写入日志或将这条消息写入数据库，短信接口
            e.printStackTrace();
        }
        //  确认消费者消费消息！
        channel.basicAck(message.getMessageProperties().getDeliveryTag(),false);
    }

    //  编写商品下架代码
    @SneakyThrows
    @RabbitListener(bindings = @QueueBinding(
            value = @Queue(value = MqConst.QUEUE_GOODS_LOWER,durable = "true",autoDelete = "false"),
            exchange = @Exchange(value = MqConst.EXCHANGE_DIRECT_GOODS),
            key = {MqConst.ROUTING_GOODS_LOWER}
    ))
    public void lowerGoodsToEs (Long skuId, Message message, Channel channel){
        try {
            //  判断skuId
            if (skuId!=null){
                //  调用商品下架方法
                searchService.lowerGoods(skuId);
            }
        } catch (Exception e) {
            //  写入日志或将这条消息写入数据库，短信接口
            e.printStackTrace();
        }
        //  消息确认
        channel.basicAck(message.getMessageProperties().getDeliveryTag(),false);
    }
}
