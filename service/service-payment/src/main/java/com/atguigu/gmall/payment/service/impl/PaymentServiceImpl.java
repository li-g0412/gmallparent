package com.atguigu.gmall.payment.service.impl;

import com.atguigu.gmall.common.constant.MqConst;
import com.atguigu.gmall.common.service.RabbitService;
import com.atguigu.gmall.model.enums.PaymentStatus;
import com.atguigu.gmall.model.order.OrderInfo;
import com.atguigu.gmall.model.payment.PaymentInfo;
import com.atguigu.gmall.payment.mapper.PaymentInfoMapper;
import com.atguigu.gmall.payment.service.PaymentService;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.Map;

@Service
public class PaymentServiceImpl implements PaymentService {

    @Autowired
    private PaymentInfoMapper paymentInfoMapper;

    @Autowired
    private RabbitService rabbitService;


    @Override
    public void savePaymentInfo(OrderInfo orderInfo,String paymentType) {
        //  支付方式从页面获取！  PaymentType.ALIPAY 我们电商使用支付宝一种支付方式就可以！
        /*
            1.  声明一个paymentInfo 对象 ，这个对象中必须有数据！
            2.  调用mapper 进行插入数据！
            3.  细节：订单Id，支付方式 这个两个条件组成的数据只能有一条数据！
         */
        //  通过sql 语句查询！
        QueryWrapper<PaymentInfo> paymentInfoQueryWrapper = new QueryWrapper<>();
        paymentInfoQueryWrapper.eq("order_id",orderInfo.getId());
        paymentInfoQueryWrapper.eq("payment_type",paymentType);
        PaymentInfo paymentInfoQuery = paymentInfoMapper.selectOne(paymentInfoQueryWrapper);
        //  判断是否有数据
        if (paymentInfoQuery!=null){
            return;
        }
        PaymentInfo paymentInfo = new PaymentInfo();
        paymentInfo.setOutTradeNo(orderInfo.getOutTradeNo());
        paymentInfo.setOrderId(orderInfo.getId());
        paymentInfo.setPaymentType(paymentType);
//        paymentInfo.setTradeNo();
        paymentInfo.setTotalAmount(orderInfo.getTotalAmount());
        paymentInfo.setSubject(orderInfo.getTradeBody());
        paymentInfo.setPaymentStatus(PaymentStatus.UNPAID.name());
        paymentInfo.setCreateTime(new Date());

        paymentInfoMapper.insert(paymentInfo);
    }

    @Override
    public PaymentInfo getPaymentInfo(String outTradeNo, String name) {
        //  select * from payment_info where out_trade_no = ? and payment_type = ?
        QueryWrapper<PaymentInfo> paymentInfoQueryWrapper = new QueryWrapper<>();
        paymentInfoQueryWrapper.eq("out_trade_no",outTradeNo);
        paymentInfoQueryWrapper.eq("payment_type",name);
        return paymentInfoMapper.selectOne(paymentInfoQueryWrapper);
    }

    @Override
    public void paySuccess(String outTradeNo, String name, Map<String, String> paramMap) {
        //  通过outTradeNo，name 查询到paymentInfo 对象！
        PaymentInfo paymentInfoQuery = getPaymentInfo(outTradeNo, name);
        //  判断
        if("PAID".equals(paymentInfoQuery.getPaymentStatus()) || "CLOSED".equals(paymentInfoQuery.getPaymentStatus())){
            return;
        }
        //  调用mapper
        PaymentInfo paymentInfo = new PaymentInfo();
        paymentInfo.setCallbackTime(new Date());
        paymentInfo.setTradeNo(paramMap.get("trade_no"));
        paymentInfo.setPaymentStatus(PaymentStatus.PAID.name());
        paymentInfo.setCallbackContent(paramMap.toString());
        //  第一个参数设置更新的内容，第二个参数表示更新的条件！
        //        QueryWrapper<PaymentInfo> paymentInfoQueryWrapper = new QueryWrapper<>();
        //        paymentInfoQueryWrapper.eq("out_trade_no",outTradeNo);
        //        paymentInfoQueryWrapper.eq("payment_type",name);
        //        paymentInfoMapper.update(paymentInfo,paymentInfoQueryWrapper);

        this.updatePaymentInfo(outTradeNo,name,paymentInfo);

        //  发送消息给订单！ 发送消息的内容？
        rabbitService.sendMessage(MqConst.EXCHANGE_DIRECT_PAYMENT_PAY,MqConst.ROUTING_PAYMENT_PAY,paymentInfoQuery.getOrderId());

    }
    //  更新交易状态记录！
    public void updatePaymentInfo(String outTradeNo, String name, PaymentInfo paymentInfo) {
        QueryWrapper<PaymentInfo> paymentInfoQueryWrapper = new QueryWrapper<>();
        paymentInfoQueryWrapper.eq("out_trade_no",outTradeNo);
        paymentInfoQueryWrapper.eq("payment_type",name);
        paymentInfoMapper.update(paymentInfo,paymentInfoQueryWrapper);


    }
}
