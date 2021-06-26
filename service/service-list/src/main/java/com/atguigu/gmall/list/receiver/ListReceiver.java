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

    //开启消息监听
    @RabbitListener(bindings = @QueueBinding(
            value = @Queue(value = MqConst.QUEUE_GOODS_UPPER, durable = "true", autoDelete = "false"),
            exchange = @Exchange(value = MqConst.EXCHANGE_DIRECT_GOODS),
            key = {MqConst.ROUTING_GOODS_UPPER}
    ))
    @SneakyThrows
    public void upperGoods(Long skuId, Message message, Channel channel){
        //获取到skuId并判断
        try {
            if (null != skuId) {
                //调用商品上架的方法
                searchService.upperGoods(skuId);
            }
        }catch (Exception e){
            //写入日志或将这条消息写入数据库
            e.printStackTrace();
        }
        //确认消费者消费信息
        channel.basicAck(message.getMessageProperties().getDeliveryTag(),false);
    }

    //编写商品下架操作
    @RabbitListener(bindings = @QueueBinding(
            value = @Queue(value = MqConst.QUEUE_GOODS_LOWER, durable = "true", autoDelete = "false"),
            exchange = @Exchange(value = MqConst.EXCHANGE_DIRECT_GOODS),
            key = {MqConst.ROUTING_GOODS_LOWER}
    ))
    @SneakyThrows
    public void loverGoodsToEs(Long skuId, Message message, Channel channel){
        //获取到skuId并判断
        try {
            if (null != skuId) {
                //调用商品上架的方法
                searchService.upperGoods(skuId);
            }
        }catch (Exception e){
                e.printStackTrace();
            }
        //确认消费者消费信息
        channel.basicAck(message.getMessageProperties().getDeliveryTag(), false);
        }
}
