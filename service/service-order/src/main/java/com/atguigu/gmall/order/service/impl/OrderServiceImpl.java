package com.atguigu.gmall.order.service.impl;

import com.alibaba.fastjson.JSON;
import com.atguigu.gmall.common.constant.MqConst;
import com.atguigu.gmall.common.service.RabbitService;
import com.atguigu.gmall.common.util.HttpClientUtil;
import com.atguigu.gmall.model.enums.OrderStatus;
import com.atguigu.gmall.model.enums.PaymentWay;
import com.atguigu.gmall.model.enums.ProcessStatus;
import com.atguigu.gmall.model.order.OrderDetail;
import com.atguigu.gmall.model.order.OrderInfo;
import com.atguigu.gmall.order.mapper.OrderDetailMapper;
import com.atguigu.gmall.order.mapper.OrderInfoMapper;
import com.atguigu.gmall.order.service.OrderService;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

@Service
public class OrderServiceImpl extends ServiceImpl<OrderInfoMapper,OrderInfo> implements OrderService {

    @Autowired
    private OrderInfoMapper orderInfoMapper;

    @Autowired
    private OrderDetailMapper orderDetailMapper;

    @Autowired
    private RedisTemplate redisTemplate;

    @Value("${ware.url}")
    private String wareUrl;

    @Autowired
    private RabbitService rabbitService;

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
        Long orderId = orderInfo.getId();
        //  发送消息！
        this.rabbitService.sendDelayMessage(MqConst.EXCHANGE_DIRECT_ORDER_CANCEL,MqConst.ROUTING_ORDER_CANCEL,orderId,MqConst.DELAY_TIME);
        //  返回订单的Id
        return orderId;
    }

    @Override
    public String getTradeNo(String userId) {
        //  定义生成的流水号
        String tradeNo = UUID.randomUUID().toString();

        //  还需将流水号放入缓存！
        String tradeNoKey = "tradeNo:"+userId;
        redisTemplate.opsForValue().set(tradeNoKey,tradeNo);

        //  返回流水号
        return tradeNo;
    }

    @Override
    public Boolean checkTradeNo(String tradeNo, String userId) {
        //  获取到key
        String tradeNoKey = "tradeNo:"+userId;
        //  获取缓存的数据
        String tradeNoRedis = (String) redisTemplate.opsForValue().get(tradeNoKey);
        //  返回比较结果
        return tradeNo.equals(tradeNoRedis);
    }

    @Override
    public void deleteTradeNo(String userId) {
        //  获取到key
        String tradeNoKey = "tradeNo:"+userId;
        //  删除数据
        redisTemplate.delete(tradeNoKey);
    }

    @Override
    public Boolean checkStock(Long skuId, Integer skuNum) {
        //  http://localhost:9001/hasStock?skuId=10221&num=2
        //  用httpClient wareUrl=http://localhost:9001
        String result = HttpClientUtil.doGet(wareUrl + "/hasStock?skuId=" + skuId + "&num=" + skuNum);
        //  0：无库存   1：有库存

        return "1".equals(result);
    }

    @Override
    public void execExpiredOrder(Long orderId) {
        //  后续我们会有很多类似的更新操作！ 进度状态中能够获取到订单状态！
        this.updateOrderStatus(orderId,ProcessStatus.CLOSED);
    }

    //  更进订单状态！
    public void updateOrderStatus(Long orderId, ProcessStatus processStatus) {
        //  update order_info set order_status = ? ,process_status = ? where id = ?
        OrderInfo orderInfo = new OrderInfo();
        orderInfo.setId(orderId);
        orderInfo.setOrderStatus(processStatus.getOrderStatus().name());
        orderInfo.setProcessStatus(processStatus.name());
        orderInfoMapper.updateById(orderInfo);
    }

    @Override
    public OrderInfo getOrderInfo(Long orderId) {
        //  select  * from order_info where id = orderId;

        OrderInfo orderInfo = orderInfoMapper.selectById(orderId);
        if (orderInfo!=null){
            //  select * from order_detail where order_id = orderId;
            QueryWrapper<OrderDetail> orderDetailQueryWrapper = new QueryWrapper<>();
            orderDetailQueryWrapper.eq("order_id",orderId);
            List<OrderDetail> orderDetailList = orderDetailMapper.selectList(orderDetailQueryWrapper);
            orderInfo.setOrderDetailList(orderDetailList);
        }
        //  返回数据
        return orderInfo;
    }

    @Override
    public void sendOrderStatus(Long orderId) {
        //  修改订单状态！
        this.updateOrderStatus(orderId,ProcessStatus.NOTIFIED_WARE);
        //  调用一个发送的方法! Json 数据类型
        String wareJson = this.initWareOrder(orderId);
        //  发送消息
        rabbitService.sendMessage(MqConst.EXCHANGE_DIRECT_WARE_STOCK,MqConst.ROUTING_WARE_STOCK,wareJson);
    }

    //  这个方法返回Json 字符串
    private String initWareOrder(Long orderId) {
        // Json 字符串是由orderInfo ，orderDetail 部分字段组成！
        OrderInfo orderInfo = this.getOrderInfo(orderId);
        //  将 orderInfo 中部分属性变为Json 字符串！
        Map map = this.initWareOrder(orderInfo);
        //  JSON.toJSONString(orderInfo); 不能这么转！
        return JSON.toJSONString(map);
    }

    /**
     * 将orderInfo 转换为Map集合
     * @param orderInfo
     * @return
     */
    public Map initWareOrder(OrderInfo orderInfo){
        Map map = new HashMap();
        map.put("orderId",orderInfo.getId());
        map.put("consignee", orderInfo.getConsignee());
        map.put("consigneeTel", orderInfo.getConsigneeTel());
        map.put("orderComment", orderInfo.getOrderComment());
        map.put("orderBody", orderInfo.getTradeBody());
        map.put("deliveryAddress", orderInfo.getDeliveryAddress());
        map.put("paymentWay", "2");
        map.put("wareId", orderInfo.getWareId());// 仓库Id ，减库存拆单时需要使用！
        //  details:[{skuId:101,skuNum:1,skuName:’小米手64G’},{skuId:201,skuNum:1,skuName:’索尼耳机’}]
        //  数据来源于orderDetailList
        List<OrderDetail> orderDetailList = orderInfo.getOrderDetailList();
        //  声明一个集合来存储订单明细需要的数据！
        ArrayList<Map> maps = new ArrayList<>();
        //  循环遍历订单明细！
        for (OrderDetail orderDetail : orderDetailList) {
            HashMap<String, Object> details = new HashMap<>();
            details.put("skuId",orderDetail.getSkuId());
            details.put("skuNum",orderDetail.getSkuNum());
            details.put("skuName",orderDetail.getSkuName());
            //  将details 放入一个集合
            maps.add(details);
        }
        map.put("details",maps);
        //  返回map 集合
        return map;
    }


    @Override
    public List<OrderInfo> orderSplit(String orderId, String wareSkuMap) {
        return null;
    }

}
