package com.atguigu.gmall.activity.controller;

import com.atguigu.gmall.activity.service.SeckillGoodsService;
import com.atguigu.gmall.activity.util.CacheHelper;
import com.atguigu.gmall.common.constant.MqConst;
import com.atguigu.gmall.common.result.Result;
import com.atguigu.gmall.common.result.ResultCodeEnum;
import com.atguigu.gmall.common.service.RabbitService;
import com.atguigu.gmall.common.util.AuthContextHolder;
import com.atguigu.gmall.common.util.DateUtil;
import com.atguigu.gmall.common.util.MD5;
import com.atguigu.gmall.model.activity.SeckillGoods;
import com.atguigu.gmall.model.activity.UserRecode;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import java.util.Date;
import java.util.List;

/**
 * @author atguigu-mqx
 */
@RestController
@RequestMapping("/api/activity/seckill")
public class SeckillGoodsApiController {

    @Autowired
    private SeckillGoodsService seckillGoodsService;

    @Autowired
    private RabbitService rabbitService;

    //  定义数据接口！
    @GetMapping("/findAll")
    public Result findAll(){
        //  获取数据
        List<SeckillGoods> seckillGoodsList = seckillGoodsService.findAll();

        //  返回
        return Result.ok(seckillGoodsList);
    }

    //  根据skuId 获取秒杀详情
    @GetMapping("/getSeckillGoods/{skuId}")
    public Result getSeckillGoods(@PathVariable Long skuId){
        //  获取数据
        SeckillGoods seckillGoods = seckillGoodsService.getSeckillGoods(skuId);

        return Result.ok(seckillGoods);
    }

    //  http://api.gmall.com/api/activity/seckill/auth/getSeckillSkuIdStr/46
    @GetMapping("auth/getSeckillSkuIdStr/{skuId}")
    public Result getSeckillSkuIdStr(@PathVariable Long skuId, HttpServletRequest request){
        //  获取用户Id
        String userId = AuthContextHolder.getUserId(request);
        //  根据skuId 获取到秒杀对象
        SeckillGoods seckillGoods = seckillGoodsService.getSeckillGoods(skuId);
        //  判断
        if (seckillGoods!=null){
            //  判断是否在活动时间范围内！  startTime currentTime endTime
            Date currentTime = new Date();
            if (DateUtil.dateCompare(seckillGoods.getStartTime(),currentTime) &&
                DateUtil.dateCompare(currentTime,seckillGoods.getEndTime())){
                //  生成下单码！
                String skuIdStr = MD5.encrypt(userId);
                //  将下单码返回给页面！
                return Result.ok(skuIdStr);
            }
        }
        //  返回数据！
        return Result.fail().message("获取下单码失败!");
    }

    //  下单控制器 /api/activity/seckill/auth/seckillOrder/{skuId}?skuIdStr=xxxx!
    @PostMapping("auth/seckillOrder/{skuId}")
    public Result seckillOrder(@PathVariable Long skuId,HttpServletRequest request){
        //  获取用户通过url 传递的下单码
        String skuIdStr = request.getParameter("skuIdStr");
        //  验证下单码：再次获取用户Id
        String userId = AuthContextHolder.getUserId(request);
        //  校验下单码
        if (!skuIdStr.equals(MD5.encrypt(userId))){
            //  验证失败！
            return Result.build(null, ResultCodeEnum.SECKILL_ILLEGAL);
        }
        //  校验状态位：
        String status = (String) CacheHelper.get(skuId.toString());
        //  判断
        if(StringUtils.isEmpty(status)){
            //  校验失败！
            return Result.build(null, ResultCodeEnum.SECKILL_ILLEGAL);
        }else if("1".equals(status)){
            //  有商品，可以秒杀！
            UserRecode userRecode = new UserRecode();
            userRecode.setUserId(userId);
            userRecode.setSkuId(skuId);

            //  将这个对象放入mq 中！
            this.rabbitService.sendMessage(MqConst.EXCHANGE_DIRECT_SECKILL_USER,MqConst.ROUTING_SECKILL_USER,userRecode);
            //  正常秒杀成功，都应该减库存！在这个地方，只是将用户的进步信息放入了mq！

        }else {
            return Result.build(null, ResultCodeEnum.SECKILL_FINISH);
        }
        //  返回数据！
        return Result.ok();
    }

    //  /api/activity/seckill/auth/checkOrder/{skuId}
    //  检查秒杀状态
    @GetMapping("auth/checkOrder/{skuId}")
    public Result checkOrder(@PathVariable Long skuId,HttpServletRequest request){
        //  获取userId
        String userId = AuthContextHolder.getUserId(request);
        //  返回数据！
        return seckillGoodsService.checkOrder(skuId,userId);
    }

}
