package com.atguigu.gmall.order.controller;

import com.alibaba.fastjson.JSON;
import com.atguigu.gmall.cart.client.CartFeignClient;
import com.atguigu.gmall.common.result.Result;
import com.atguigu.gmall.common.util.AuthContextHolder;
import com.atguigu.gmall.model.cart.CartInfo;
import com.atguigu.gmall.model.order.OrderDetail;
import com.atguigu.gmall.model.order.OrderInfo;
import com.atguigu.gmall.model.user.UserAddress;
import com.atguigu.gmall.order.service.OrderService;
import com.atguigu.gmall.product.client.ProductFeignClient;
import com.atguigu.gmall.user.client.UserFeignClient;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ThreadPoolExecutor;

@RestController
@RequestMapping("api/order") // api/order/orderSplit
public class OrderApiController {

    //  获取到service-user 远程调用服务！
    @Autowired
    private UserFeignClient userFeignClient;

    @Autowired
    private CartFeignClient cartFeignClient;

    @Autowired
    private OrderService orderService;

    @Autowired
    private ProductFeignClient productFeignClient;

    @Autowired
    private ThreadPoolExecutor threadPoolExecutor;

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
        //  获取流水号 tradeNo
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

        //  获取页面的流水号与缓存的流水号进行比较！
        String tradeNo = request.getParameter("tradeNo");

        Boolean flag = orderService.checkTradeNo(tradeNo, userId);
        //  判断
        //        if (flag){
        //            //  正常提交
        //        }else {
        //            //  非法提交！
        //        }

        if (!flag){
            //  返回非法提交信息
            return Result.fail().message("不可回退提交订单!");
        }
        //  删除缓存流水号
        orderService.deleteTradeNo(userId);

        //  验证库存！ 远程调用 传入 skuId,skuNum !
        List<OrderDetail> orderDetailList = orderInfo.getOrderDetailList();
        //  声明一个集合来存储错误信息的！
        ArrayList<String> strList = new ArrayList<>();
        ArrayList<CompletableFuture> completableFutureArrayList = new ArrayList<>();
        //  循环遍历
        for (OrderDetail orderDetail : orderDetailList) {
            //  验证库存！
            CompletableFuture<Void> stockCompletableFuture = CompletableFuture.runAsync(() -> {
                Boolean result = orderService.checkStock(orderDetail.getSkuId(), orderDetail.getSkuNum());
                //  判断
                if (!result) {
                    //  Result.fail().message(orderDetail.getSkuName()+"库存不足!");
                    strList.add(orderDetail.getSkuName() + "库存不足!");
                }
            },threadPoolExecutor);
            //  将验证库存的对象添加到集合
            completableFutureArrayList.add(stockCompletableFuture);

            //  验证价格！
            CompletableFuture<Void> priceCompletableFuture = CompletableFuture.runAsync(() -> {
                //  验证商品的价格：
                BigDecimal orderPrice = orderDetail.getOrderPrice();
                //  订单价格与实时价格比较！
                BigDecimal skuPrice = productFeignClient.getSkuPrice(orderDetail.getSkuId());
                //  判断
                if (orderPrice.compareTo(skuPrice)!=0){
                    //  价格有变动！
                    //  说明购物车中的价格有变动！更改购物车的价格！
                    cartFeignClient.loadCartCache(userId);
                    //  return Result.fail().message(orderDetail.getSkuName()+" 价格有变动!");
                    strList.add(orderDetail.getSkuName()+" 价格有变动!");
                }
            },threadPoolExecutor);
            //  将验证价格对象放入集合
            completableFutureArrayList.add(priceCompletableFuture);

            //            //  调用库存接口
            //            Boolean result = orderService.checkStock(orderDetail.getSkuId(),orderDetail.getSkuNum());
            //            //  判断
            //            if (!result){
            //                return Result.fail().message(orderDetail.getSkuName()+"库存不足!");
            //            }
            //            //  验证商品的价格：
            //            BigDecimal orderPrice = orderDetail.getOrderPrice();
            //            //  订单价格与实时价格比较！
            //            BigDecimal skuPrice = productFeignClient.getSkuPrice(orderDetail.getSkuId());
            //            //  判断
            //            if (orderPrice.compareTo(skuPrice)!=0){
            //                //  价格有变动！
            //                //  说明购物车中的价格有变动！更改购物车的价格！
            //                cartFeignClient.loadCartCache(userId);
            //                return Result.fail().message(orderDetail.getSkuName()+" 价格有变动!");
            //            }
        }

        //  多任务组合！ 将集合转换为动态数组
        CompletableFuture.allOf(completableFutureArrayList.toArray(new CompletableFuture[completableFutureArrayList.size()])).join();
        //  判断输入信息！strList
        if (strList.size()>0){
            //  说明有异常！
            //  输出集合中的数据！ 将集合中的数据使用,进行连接！
            return Result.fail().message(StringUtils.join(strList,","));
        }
        //  user_id{controller 能够获取到！}
        orderInfo.setUserId(Long.parseLong(userId));
        //  调用服务层方法
        Long orderId = orderService.saveOrder(orderInfo);

        //  将orderId 返回即可！
        return Result.ok(orderId);
    }

    //  根据订单Id 查询订单信息
    @GetMapping("inner/getOrderInfo/{orderId}")
    public OrderInfo getOrderInfo(@PathVariable Long orderId){
        return orderService.getOrderInfo(orderId);
    }

    //  拆单的数据接口！
    //  http://localhost:8204/api/order/orderSplit?orderId=xxx&wareSkuMap=xxx
    @PostMapping("orderSplit")
    public String orderSplit(HttpServletRequest request){
        //  获取到订单Id
        String orderId = request.getParameter("orderId");
        //  仓库Id 与 商品skuId 的对照关系 [{"wareId":"1","skuIds":["2","10"]},{"wareId":"2","skuIds":["3"]}]
        String wareSkuMap = request.getParameter("wareSkuMap");

        //  声明一个集合来存储map
        ArrayList<Map> maps = new ArrayList<>();
        //  先根据上述两个参数得到子订单集合：
        List<OrderInfo> orderInfoList = orderService.orderSplit(orderId,wareSkuMap);
        //  将这个子订单集合循环遍历
        for (OrderInfo orderInfo : orderInfoList) {
            //  orderInfo 转换的map 集合
            Map map = orderService.initWareOrder(orderInfo);
            maps.add(map);
        }
        //  返回子订单Json字符串
        return JSON.toJSONString(maps);
    }

}
