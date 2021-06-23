package com.atguigu.gmall.cart.controller;

import com.atguigu.gmall.cart.service.CartService;
import com.atguigu.gmall.common.result.Result;
import com.atguigu.gmall.common.util.AuthContextHolder;
import com.atguigu.gmall.model.cart.CartInfo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import java.util.List;

@RestController
@RequestMapping("api/cart")
public class CartApiController {

    @Autowired
    private CartService cartService;

    //  映射路径：
    @RequestMapping("addToCart/{skuId}/{skuNum}")
    public Result addToCart(@PathVariable Long skuId,
                            @PathVariable Integer skuNum,
                            HttpServletRequest request){

        //  添加购物车的时候，需要userId ,userTempId
        String userId = AuthContextHolder.getUserId(request);
        //  用户此时是未登录状态！
        if (StringUtils.isEmpty(userId)){
            //  获取到临时的！
            userId = AuthContextHolder.getUserTempId(request);
        }
        //  添加购物车
        cartService.addToCart(skuId,userId,skuNum);

        //  默认返回ok！
        return Result.ok();
    }

    //  查询购物车列表：
    @GetMapping("cartList")
    public Result getCartList(HttpServletRequest request){
        //  获取用户Id，获取临时用户Id
        String userId = AuthContextHolder.getUserId(request);

        String userTempId = AuthContextHolder.getUserTempId(request);
        //  调用服务层方法
        List<CartInfo> cartList = cartService.getCartList(userId, userTempId);

        //  返回数据
        return Result.ok(cartList);
    }

    //  选中状态
    @GetMapping("checkCart/{skuId}/{isChecked}")
    public Result checkCart(@PathVariable Long skuId,
                            @PathVariable Integer isChecked,
                            HttpServletRequest request){

        String userId = AuthContextHolder.getUserId(request);
        //  判断
        if (StringUtils.isEmpty(userId)){
            userId = AuthContextHolder.getUserTempId(request);
        }
        //  调用服务层方法
        cartService.checkCart(userId,isChecked,skuId);
        return Result.ok();
    }

    //  删除购物车：
    @DeleteMapping("deleteCart/{skuId}")
    public Result deleteCart(@PathVariable Long skuId,
                             HttpServletRequest request){
        String userId = AuthContextHolder.getUserId(request);
        //  判断
        if (StringUtils.isEmpty(userId)){
            userId = AuthContextHolder.getUserTempId(request);
        }
        //  调用服务层方法
        cartService.deleteCart(skuId,userId);
        return Result.ok();

    }

    //  获取选中状态的购物车
    @GetMapping("getCartCheckedList/{userId}")
    public List<CartInfo> getCartCheckedList(@PathVariable String userId){
        return cartService.getCartCheckedList(userId);
    }
}
