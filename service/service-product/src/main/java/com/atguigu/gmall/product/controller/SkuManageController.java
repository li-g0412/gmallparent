package com.atguigu.gmall.product.controller;

import com.atguigu.gmall.common.result.Result;
import com.atguigu.gmall.model.product.SkuInfo;
import com.atguigu.gmall.product.service.ManageService;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

/**
 * @author atguigu-mqx
 */
@RestController
@RequestMapping("admin/product")
public class SkuManageController {

    @Autowired
    private ManageService manageService;

    //  http://localhost/admin/product/saveSkuInfo
    //  json --- > javaObject
    @PostMapping("saveSkuInfo")
    public Result saveSkuInfo(@RequestBody SkuInfo skuInfo){
        //  调用服务层方法
        manageService.saveSkuInfo(skuInfo);
        return Result.ok();
    }

    //  获取sku列表
    //  http://api.gmall.com/admin/product/list/{page}/{limit}
    @GetMapping("list/{page}/{limit}")
    public Result getSkuInfoList(@PathVariable Long page,
                                 @PathVariable Long limit){

        Page<SkuInfo> skuInfoPage = new Page<>(page,limit);
        //  调用服务层的方法
        IPage<SkuInfo> skuInfoIPage = manageService.getSkuInfoList(skuInfoPage);
        return Result.ok(skuInfoIPage);
    }

    //  上架
    //  http://api.gmall.com/admin/product/onSale/{skuId}
    @GetMapping("onSale/{skuId}")
    public Result onSale(@PathVariable Long skuId){
        //  调用服务层的方法
        manageService.onSale(skuId);
        return Result.ok();
    }

    //  下架
    //  http://api.gmall.com/admin/product/cancelSale/{skuId}
    @GetMapping("cancelSale/{skuId}")
    public Result cancelSale(@PathVariable Long skuId){
        //  调用服务层的方法
        manageService.cancelSale(skuId);
        return Result.ok();
    }


}
