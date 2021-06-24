package com.atguigu.gmall.order.service.impl;

import com.atguigu.gmall.common.util.HttpClientUtil;
import com.atguigu.gmall.model.enums.OrderStatus;
import com.atguigu.gmall.model.enums.PaymentWay;
import com.atguigu.gmall.model.enums.ProcessStatus;
import com.atguigu.gmall.model.order.OrderDetail;
import com.atguigu.gmall.model.order.OrderInfo;
import com.atguigu.gmall.order.mapper.OrderDetailMapper;
import com.atguigu.gmall.order.mapper.OrderInfoMapper;
import com.atguigu.gmall.order.service.OrderService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

@Service
public class OrderServiceImpl implements OrderService {

    @Autowired
    private OrderInfoMapper orderInfoMapper;

    @Autowired
    private OrderDetailMapper orderDetailMapper;

    @Autowired
    private RedisTemplate redisTemplate;

    @Value("${ware.url}")
    private String WARE_URL;
    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long saveOrder(OrderInfo orderInfo) {
        //  本质：执行insert into 操作！
        //  orderInfo orderDetail;
        //  orderInfo 中 缺少的字段！total_amount，order_status，user_id{controller 能够获取到！}
        //  payment_way,out_trade_no,trade_body,create_time,expire_time,process_status
        orderInfo.sumTotalAmount();
        orderInfo.setOrderStatus(OrderStatus.UNPAID.name());
        orderInfo.setPaymentWay(PaymentWay.ONLINE.name());
        String outTradeNo = "ATGUIGU" + System.currentTimeMillis() + "" + new Random().nextInt(1000);
        //  第三方交易编号！ 支付的时候使用的！
        orderInfo.setOutTradeNo(outTradeNo);
        //  第一种方式：可以给一个固定值！
        //  orderInfo.setTradeBody("买买买!!!");
        //  第二种方式：可以将商品的名称进行拼接！
        List<OrderDetail> orderDetailList = orderInfo.getOrderDetailList();
        StringBuilder sb = new StringBuilder();
        for (OrderDetail orderDetail : orderDetailList) {
            //  拼接商品的名称
            sb.append(orderDetail.getSkuName());
        }
        //  截取字符串！
        if (sb.length()>200){
            orderInfo.setTradeBody(sb.substring(0,200));
        }else {
            orderInfo.setTradeBody(sb.toString());
        }

        //  创建时间
        orderInfo.setCreateTime(new Date());

        Calendar calendar = Calendar.getInstance();
        calendar.add(Calendar.DATE,1);
        //  过期时间 + 1 天
        orderInfo.setExpireTime(calendar.getTime());

        //  设置进度状态
        orderInfo.setProcessStatus(ProcessStatus.UNPAID.name());

        orderInfoMapper.insert(orderInfo);

        //  保存orderDetail!
        List<OrderDetail> orderDetailLists = orderInfo.getOrderDetailList();
        //  循环遍历
        for (OrderDetail orderDetail : orderDetailLists) {
            // order_id
            orderDetail.setOrderId(orderInfo.getId());
            // create_time
            orderDetail.setCreateTime(new Date());
            orderDetailMapper.insert(orderDetail);
        }
        //  返回订单的Id
        return orderInfo.getId();
    }

    //生成流水号
    @Override
    public String getTradeNo(String userId) {
        //定义生成的流水号
        String tradeNo = UUID.randomUUID().toString();
        //将流水号放入缓存
        String tradeNoKey = "tradeNo:"+userId;
        redisTemplate.opsForValue().set(tradeNoKey,tradeNo);

        //返回流水号
        return tradeNo;
    }

    //比较流水号
    @Override
    public Boolean checkTradeNo(String tradeNo, String userId) {
        //获取到缓存中的key
        String tradeNoKey = "tradeNo:"+userId;
        String tradeNoRedis = (String) redisTemplate.opsForValue().get(tradeNoKey);
        return tradeNo.equals(tradeNoRedis);
    }

    //删除流水号
    @Override
    public void deleteTradeNo(String userId) {
        // 定义key
        String tradeNoKey = "user:" + userId + ":tradeCode";
        // 删除数据
        redisTemplate.delete(tradeNoKey);
    }

    @Override
    public Boolean checkStock(Long skuId, Integer skuNum) {
        //
        String result = HttpClientUtil.doGet(WARE_URL + "/hasStock?skuId=" + skuId + "&num=" + skuNum);

        return "1".equals(result);
    }
}
