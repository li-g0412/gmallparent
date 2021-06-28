package com.atguigu.gmall.order.receiver;

import com.alibaba.fastjson.JSON;
import com.atguigu.gmall.common.constant.MqConst;
import com.atguigu.gmall.model.enums.ProcessStatus;
import com.atguigu.gmall.model.order.OrderInfo;
import com.atguigu.gmall.order.service.OrderService;
import com.rabbitmq.client.Channel;
import lombok.SneakyThrows;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.Exchange;
import org.springframework.amqp.rabbit.annotation.Queue;
import org.springframework.amqp.rabbit.annotation.QueueBinding;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
public class OrderReceiver {

    @Autowired
    private OrderService orderService;

    //  监听消息
    @SneakyThrows
    @RabbitListener(queues = MqConst.QUEUE_ORDER_CANCEL)
    public void orderCancel(Long orderId, Message message, Channel channel){
        try {
            //  业务逻辑
            //  判断订单Id
            if (orderId!=null){
                //  判断当前的订单状态！order_status ， process_status 未支付
                //  select * from order_info where id = ?;
                OrderInfo orderInfo = orderService.getById(orderId);
                if (orderInfo!=null){
                    if ("UNPAID".equals(orderInfo.getOrderStatus()) && "UNPAID".equals(orderInfo.getProcessStatus())){
                        //  关闭订单！
                        orderService.execExpiredOrder(orderId);
                    }
                }
            }
        } catch (Exception e) {
            //  写入日志...
            e.printStackTrace();
        }
        //  手动确认
        channel.basicAck(message.getMessageProperties().getDeliveryTag(),false);
    }

    //  监听支付发送过来的消息
    @SneakyThrows
    @RabbitListener(bindings = @QueueBinding(
            value=@Queue(value = MqConst.QUEUE_PAYMENT_PAY,durable = "true",autoDelete = "false"),
            exchange = @Exchange(value = MqConst.EXCHANGE_DIRECT_PAYMENT_PAY),
            key = {MqConst.ROUTING_PAYMENT_PAY}
    ))
    public void paymentSuccess(Long orderId ,Message message, Channel channel){
        try {
            //  判断
            if (orderId!=null){
                OrderInfo orderInfo = orderService.getById(orderId);
                //  正常过来一定的未支付！
                if("UNPAID".equals(orderInfo.getOrderStatus()) && "UNPAID".equals(orderInfo.getProcessStatus())){
                    //  本质更新订单状态！
                    orderService.updateOrderStatus(orderId, ProcessStatus.PAID);
                    //  发送一个消息，通知库存，让库存系统减库存！
                    orderService.sendOrderStatus(orderId);
                }
            }
        } catch (Exception e) {
            //  写入日志或数据库，后续处理！
            e.printStackTrace();
        }

        //  手动确认消息
        channel.basicAck(message.getMessageProperties().getDeliveryTag(),false);
    }


    //  在此监听减库存结果的消息！
    //   orderId , status
    @SneakyThrows
    @RabbitListener(bindings = @QueueBinding(
            value = @Queue(value = MqConst.QUEUE_WARE_ORDER,durable = "true",autoDelete = "false"),
            exchange = @Exchange(value = MqConst.EXCHANGE_DIRECT_WARE_ORDER),
            key = {MqConst.ROUTING_WARE_ORDER}
    ))
    public void wareOrder(String wareJson , Message message,Channel channel){
        try {
            //  最重要根据status 判断是否减库存成功！{"orderId":"1","status":"DEDUCTED"}
            //  需要将wareJson 转换为map
            Map map = JSON.parseObject(wareJson, Map.class);
            //  获取到orderId，status
            String orderId = (String) map.get("orderId");
            String status = (String) map.get("status");

            //  判断是否减库存成功！
            if("DEDUCTED".equals(status)){
                //  表示减库存成功！
                orderService.updateOrderStatus(Long.parseLong(orderId),ProcessStatus.WAITING_DELEVER);
            }else {
                //  OUT_OF_STOCK 属于超卖！ 订单，支付，库存 并不退款！而是记录消息，将写入数据库！
                //  调用库存补货， 人工客服！
                orderService.updateOrderStatus(Long.parseLong(orderId),ProcessStatus.STOCK_EXCEPTION);
            }
        } catch (NumberFormatException e) {
            //  记录日志
            e.printStackTrace();
        }
        //  手动确认
        channel.basicAck(message.getMessageProperties().getDeliveryTag(),false);
    }


}
