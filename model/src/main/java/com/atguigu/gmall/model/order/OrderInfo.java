package com.atguigu.gmall.model.order;

import com.atguigu.gmall.model.activity.CouponInfo;
import com.atguigu.gmall.model.base.BaseEntity;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.math.BigDecimal;
import java.util.Date;
import java.util.List;

@Data
@ApiModel(description = "订单信息")
@TableName("order_info")
public class OrderInfo extends BaseEntity {
    private static final long serialVersionUID = 1L;

    @ApiModelProperty(value = "收货人")
    @TableField("consignee")
    private String consignee;

    @ApiModelProperty(value = "收件人电话")
    @TableField("consignee_tel")
    private String consigneeTel;

    @ApiModelProperty(value = "总金额")
    @TableField("total_amount")
    private BigDecimal totalAmount;

    @ApiModelProperty(value = "订单状态")
    @TableField("order_status")
    private String orderStatus;

    @ApiModelProperty(value = "用户id")
    @TableField("user_id")
    private Long userId;

    @ApiModelProperty(value = "付款方式")
    @TableField("payment_way")
    private String paymentWay;

    @ApiModelProperty(value = "送货地址")
    @TableField("delivery_address")
    private String deliveryAddress;

    @ApiModelProperty(value = "订单备注")
    @TableField("order_comment")
    private String orderComment;

    @ApiModelProperty(value = "订单交易编号（第三方支付用)")
    @TableField("out_trade_no")
    private String outTradeNo;

    @ApiModelProperty(value = "订单描述(第三方支付用)")
    @TableField("trade_body")
    private String tradeBody;

    @ApiModelProperty(value = "创建时间")
    @TableField("create_time")
    private Date createTime;

    @ApiModelProperty(value = "失效时间")
    @TableField("expire_time")
    private Date expireTime;

    @ApiModelProperty(value = "进度状态")
    @TableField("process_status")
    private String processStatus;

    @ApiModelProperty(value = "物流单编号")
    @TableField("tracking_no")
    private String trackingNo;

    @ApiModelProperty(value = "父订单编号")
    @TableField("parent_order_id")
    private Long parentOrderId;

    @ApiModelProperty(value = "图片路径")
    @TableField("img_url")
    private String imgUrl;

    @TableField(exist = false)
    private List<OrderDetail> orderDetailList;

    @TableField(exist = false)
    private String wareId;

    @ApiModelProperty(value = "地区")
    @TableField("province_id")
    private Long provinceId;

    @ApiModelProperty(value = "促销金额")
    @TableField("activity_reduce_amount")
    private BigDecimal activityReduceAmount;

    @ApiModelProperty(value = "优惠券")
    @TableField("coupon_amount")
    private BigDecimal couponAmount;

    @ApiModelProperty(value = "原价金额")
    @TableField("original_total_amount")
    private BigDecimal originalTotalAmount;

    @ApiModelProperty(value = "可退款日期（签收后30天）")
    @TableField("refundable_time")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date refundableTime;

    @ApiModelProperty(value = "运费")
    @TableField("feight_fee")
    private BigDecimal feightFee;

    @ApiModelProperty(value = "操作时间")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @TableField("operate_time")
    private Date operateTime;

    //  计算活动或者优惠劵的金额
    @TableField(exist = false)
    private List<OrderDetailVo> orderDetailVoList;

    @TableField(exist = false)
    private CouponInfo couponInfo;

    // 计算总价格
    public void sumTotalAmount(){
        BigDecimal totalAmount = new BigDecimal("0");
        BigDecimal originalTotalAmount = new BigDecimal("0");
        BigDecimal couponAmount = new BigDecimal("0");
        //  减去优惠劵
        if(null != couponInfo) {
            couponAmount = couponAmount.add(couponInfo.getReduceAmount());
            totalAmount = totalAmount.subtract(couponInfo.getReduceAmount());
        }
        //  减去活动
        if(null != this.getActivityReduceAmount()) {
            totalAmount = totalAmount.subtract(this.getActivityReduceAmount());
        }
        //  计算最后
        for (OrderDetail orderDetail : orderDetailList) {
            BigDecimal skuTotalAmount = orderDetail.getOrderPrice().multiply(new BigDecimal(orderDetail.getSkuNum()));
            originalTotalAmount = originalTotalAmount.add(skuTotalAmount);
            totalAmount = totalAmount.add(skuTotalAmount);
        }
        this.setTotalAmount(totalAmount);
        this.setOriginalTotalAmount(originalTotalAmount);
        this.setCouponAmount(couponAmount);
    }

}
