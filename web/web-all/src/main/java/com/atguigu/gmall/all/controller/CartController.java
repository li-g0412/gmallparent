package com.atguigu.gmall.all.controller;

import com.atguigu.gmall.cart.client.CartFeignClient;
import com.atguigu.gmall.model.product.SkuInfo;
import com.atguigu.gmall.product.client.ProductFeignClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;

import javax.servlet.http.HttpServletRequest;

@Controller
public class CartController {

    @Autowired
    private CartFeignClient cartFeignClient;

    @Autowired
    private ProductFeignClient productFeignClient;

    //  添加购物车！
    //  http://cart.gmall.com/addCart.html?skuId=47&skuNum=1
    @RequestMapping("addCart.html")
    public String addToCart(HttpServletRequest request){
        String skuId = request.getParameter("skuId");
        String skuNum = request.getParameter("skuNum");

        //  调用添加购物车方法
        cartFeignClient.addToCart(Long.parseLong(skuId),Integer.parseInt(skuNum));
        //  存储skuInfo ,skuNum
        SkuInfo skuInfo = productFeignClient.getSkuInfo(Long.parseLong(skuId));
        request.setAttribute("skuInfo",skuInfo);
        request.setAttribute("skuNum",skuNum);
        //  返回视图名称
        return "cart/addCart";
    }

    //  查看购物车列表 href="/cart.html"
    @RequestMapping("cart.html")
    public String cartList(){
        //  返回的购物车列表页面！
        return "cart/index";
    }
}
