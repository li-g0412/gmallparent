package com.atguigu.gmall.order.receiver;

import com.atguigu.gmall.common.constant.MqConst;
import com.atguigu.gmall.model.order.OrderInfo;
import com.atguigu.gmall.order.service.OrderService;
import com.rabbitmq.client.Channel;
import lombok.SneakyThrows;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

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
}
