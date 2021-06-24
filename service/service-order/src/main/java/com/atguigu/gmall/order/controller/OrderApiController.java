package com.atguigu.gmall.order.controller;

import com.atguigu.gmall.cart.client.CartFeignClient;
import com.atguigu.gmall.common.result.Result;
import com.atguigu.gmall.common.util.AuthContextHolder;
import com.atguigu.gmall.model.cart.CartInfo;
import com.atguigu.gmall.model.order.OrderDetail;
import com.atguigu.gmall.model.order.OrderInfo;
import com.atguigu.gmall.model.user.UserAddress;
import com.atguigu.gmall.order.service.OrderService;
import com.atguigu.gmall.user.client.UserFeignClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("api/order")
public class OrderApiController {

    //  获取到service-user 远程调用服务！
    @Autowired
    private UserFeignClient userFeignClient;

    @Autowired
    private CartFeignClient cartFeignClient;

    @Autowired
    private OrderService orderService;


    //  定义一个控制器！
    @GetMapping("auth/trade")
    public Result<Map<String,Object>> trade(HttpServletRequest request){
        //  声明一个map 集合
        HashMap<String, Object> map = new HashMap<>();
        //  获取到用户Id
        String userId = AuthContextHolder.getUserId(request);
        List<UserAddress> userAddressList = userFeignClient.findUserAddressListByUserId(userId);

        //  获取到选中购物车集合
        List<CartInfo> cartCheckedList = cartFeignClient.getCartCheckedList(userId);

        //  送货清单！ 数据是从购物车中获取的！
        ArrayList<OrderDetail> detailArrayList = new ArrayList<>();

        //  int totalNum = 0;
        //  循环遍历选中购物车集合
        for (CartInfo cartInfo : cartCheckedList) {
            OrderDetail orderDetail = new OrderDetail();
            //  属性赋值
            orderDetail.setSkuId(cartInfo.getSkuId());
            orderDetail.setSkuName(cartInfo.getSkuName());
            orderDetail.setImgUrl(cartInfo.getImgUrl());
            orderDetail.setOrderPrice(cartInfo.getSkuPrice());
            orderDetail.setSkuNum(cartInfo.getSkuNum());
            //  totalNum += cartInfo.getSkuNum();
            detailArrayList.add(orderDetail);
        }
        //  数据给web-all  ，页面需要 userAddressList ， detailArrayList ，totalNum  ，totalAmount
        map.put("userAddressList",userAddressList);
        map.put("detailArrayList",detailArrayList);
        //  第一种：单纯计算有几种skuId! 第二种就是计算每个skuId 对应的件数！
        map.put("totalNum",cartCheckedList.size());
        //  map.put("totalNum",totalNum);
        //  计算总金额：单价*数量
        OrderInfo orderInfo = new OrderInfo();
        orderInfo.setOrderDetailList(detailArrayList);
        //  计算总价格  计算完成之后赋值给当前对象 totalAmount
        orderInfo.sumTotalAmount();
        map.put("totalAmount",orderInfo.getTotalAmount());
        //获取流水号突然地No
        String tradeNo = orderService.getTradeNo(userId);
        map.put("tradeNo",tradeNo);
        return Result.ok(map);
    }

    //  http://api.gmall.com/api/order/auth/submitOrder?tradeNo=null ? 这个控制器写在哪？
    //  前端传递的是Json 字符串！
    @PostMapping("/auth/submitOrder")
    public Result submitOrder(@RequestBody OrderInfo orderInfo,HttpServletRequest request){
        //  获取到userId
        String userId = AuthContextHolder.getUserId(request);
        //获取页面的流水号和缓存的流水号比较
        String tradeNo = request.getParameter("tradeNo");
        Boolean flag = orderService.checkTradeNo(tradeNo, userId);
        if (!flag){
            //返回非法提示信息
            return Result.fail().message("不可回退提交订单");
        }
        //删除缓存流水号
        orderService.deleteTradeNo(userId);
        //验证库存远程调用
        List<OrderDetail> orderDetailList = orderInfo.getOrderDetailList();
        //循环遍历
        for (OrderDetail orderDetail : orderDetailList) {
            Boolean result = orderService.checkStock(orderDetail.getSkuId(),orderDetail.getSkuNum());
            if (!result){
                return Result.fail().message(orderDetail.getSkuName()+"库存不足");
            }
        }


        //  user_id{controller 能够获取到！}
        orderInfo.setUserId(Long.parseLong(userId));
        //  调用服务层方法
        Long orderId = orderService.saveOrder(orderInfo);

        //  将orderId 返回即可！
        return Result.ok(orderId);
    }


}
