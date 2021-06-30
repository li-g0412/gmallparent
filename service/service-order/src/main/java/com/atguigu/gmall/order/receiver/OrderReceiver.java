package com.atguigu.gmall.order.receiver;

import com.alibaba.fastjson.JSON;
import com.atguigu.gmall.common.constant.MqConst;
import com.atguigu.gmall.model.enums.ProcessStatus;
import com.atguigu.gmall.model.order.OrderInfo;
import com.atguigu.gmall.model.payment.PaymentInfo;
import com.atguigu.gmall.order.service.OrderService;
import com.atguigu.gmall.payment.client.PaymentFeignClient;
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

    @Autowired
    private PaymentFeignClient paymentFeignClient;

    //  监听消息
    @SneakyThrows
    @RabbitListener(queues = MqConst.QUEUE_ORDER_CANCEL)
    public void orderCancel(Long orderId, Message message, Channel channel){
        try {
            //  判断订单id 是否存在！
            if (orderId!=null){
                //  根据订单Id 查询订单对象
                OrderInfo orderInfo = orderService.getById(orderId);
                //  判断
                if(orderInfo!=null && "UNPAID".equals(orderInfo.getOrderStatus()) && "UNPAID".equals(orderInfo.getProcessStatus())){
                    //  关闭过期订单！ 还需要关闭对应的 paymentInfo ，还有alipay.
                    //  orderService.execExpiredOrder(orderId);
                    //  查询paymentInfo 是否存在！
                    PaymentInfo paymentInfo = paymentFeignClient.getPaymentInfo(orderInfo.getOutTradeNo());
                    //  判断 用户点击了扫码支付
                    if(paymentInfo!=null && "UNPAID".equals(paymentInfo.getPaymentStatus())){

                        //  查看是否有交易记录！
                        Boolean flag = paymentFeignClient.checkPayment(orderId);
                        //  判断
                        if (flag){
                            //  flag = true , 有交易记录
                            //  调用关闭接口！ 扫码未支付这样才能关闭成功！
                            Boolean result = paymentFeignClient.closePay(orderId);
                            //  判断
                            if (result){
                                //  result = true; 关闭成功！未付款！需要关闭orderInfo， paymentInfo，Alipay
                                orderService.execExpiredOrder(orderId,"2");
                            }else {
                                //  result = false; 表示付款！
                                //  说明已经付款了！ 正常付款成功都会走异步通知！
                            }
                        }else {
                            //  没有交易记录，不需要关闭支付！  需要关闭orderInfo， paymentInfo
                            orderService.execExpiredOrder(orderId,"2");
                        }

                    }else {
                        //  只关闭订单orderInfo！
                        orderService.execExpiredOrder(orderId,"1");
                    }
                }
            }

            //            //  业务逻辑 关闭orderInfo ,paymentInfo ,aliPay
            //            //  判断订单Id
            //            if (orderId!=null){
            //                //  判断当前的订单状态！order_status ， process_status 未支付
            //                //  select * from order_info where id = ?;
            //                OrderInfo orderInfo = orderService.getById(orderId);
            //                if (orderInfo!=null){
            //                    if ("UNPAID".equals(orderInfo.getOrderStatus()) && "UNPAID".equals(orderInfo.getProcessStatus())){
            //                        //  关闭订单！
            //                        orderService.execExpiredOrder(orderId);
            //                    }
            //                }
            //            }
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
