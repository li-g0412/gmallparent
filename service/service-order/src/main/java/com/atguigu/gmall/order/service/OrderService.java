package com.atguigu.gmall.order.service;

import com.atguigu.gmall.model.enums.ProcessStatus;
import com.atguigu.gmall.model.order.OrderInfo;
import com.baomidou.mybatisplus.extension.service.IService;

import java.util.List;
import java.util.Map;

public interface OrderService extends IService<OrderInfo> {

    //  保存订单！
    Long saveOrder(OrderInfo orderInfo);

    /**
     * 生成流水号
     * @param userId
     * @return
     */
    String getTradeNo(String userId);

    /**
     * 比较流水号
     * @param tradeNo
     * @param userId
     * @return
     */
    Boolean checkTradeNo(String tradeNo , String userId);

    /**
     * 删除缓存的流水号
     * @param userId
     */
    void deleteTradeNo(String userId);

    /**
     * 验证库存
     * @param skuId
     * @param skuNum
     * @return
     */
    Boolean checkStock(Long skuId, Integer skuNum);

    /**
     * 根据订单Id 关闭过期订单！
     * @param orderId
     */
    void execExpiredOrder(Long orderId);

    /**
     * 根据订单Id 与进度状态更新订单数据！
     * @param orderId
     * @param processStatus
     */
    void updateOrderStatus(Long orderId, ProcessStatus processStatus);

    /**
     * 根据订单Id 获取 订单数据！
     * @param orderId
     * @return
     */
    OrderInfo getOrderInfo(Long orderId);

    /**
     * 发送消息给库存系统
     * @param orderId
     */
    void sendOrderStatus(Long orderId);

    /**
     * 将orderInfo 部分数据转换为Map
     * @param orderInfo
     * @return
     */
    Map initWareOrder(OrderInfo orderInfo);

    /**
     * 拆单方法
     * @param orderId
     * @param wareSkuMap
     * @return
     */
    List<OrderInfo> orderSplit(String orderId, String wareSkuMap);

}
