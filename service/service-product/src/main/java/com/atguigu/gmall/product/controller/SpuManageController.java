package com.atguigu.gmall.product.controller;

import com.atguigu.gmall.common.result.Result;
import com.atguigu.gmall.model.product.BaseSaleAttr;
import com.atguigu.gmall.model.product.SpuImage;
import com.atguigu.gmall.model.product.SpuInfo;
import com.atguigu.gmall.model.product.SpuSaleAttr;
import com.atguigu.gmall.product.service.ManageService;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * @author atguigu-mqx
 */
@RestController
@RequestMapping("admin/product")
public class SpuManageController {

    @Autowired
    private ManageService manageService;

    //  springmvc 对象传值方式：
    //  注意参数传递的方式！
    //  http://localhost/admin/product/1/10?category3Id=62
    //  http://api.gmall.com/admin/product/{page}/{limit}?category3Id=61
    //  获取category3Id 对应的数据
    @GetMapping("{page}/{limit}")
    public Result getPage(@PathVariable Long page,
                          @PathVariable Long limit,
                          SpuInfo spuInfo){
        //  创建一个Page对象
        Page<SpuInfo> spuInfoPage = new Page<>(page,limit);
        //  调用服务层方法
        //  IPage<SpuInfo> page1 = manageService.getPage(spuInfoPage, spuInfo.getCategory3Id());
        IPage<SpuInfo> pages = manageService.getPages(spuInfoPage, spuInfo);
        //  设置返回数据
        return Result.ok(pages);
    }

    //  获取销售属性数据
    //  http://api.gmall.com/admin/product/baseSaleAttrList
    @GetMapping("baseSaleAttrList")
    public Result getBseSaleAttrList(){
        //  调用服务层方法
        List<BaseSaleAttr> baseSaleAttrList = manageService.getBseSaleAttrList();
        return Result.ok(baseSaleAttrList);
    }

    //  http://localhost/admin/product/saveSpuInfo
    //  接收传递的参数
    //  spuInfo 保存
    @PostMapping("/saveSpuInfo")
    public Result saveSpuInfo(@RequestBody SpuInfo spuInfo) {

        //  调用服务层方法
        manageService.saveSpuInfo(spuInfo);
        return Result.ok();
    }

    //  根据spuId 获取spuImage 集合
    //  http://localhost/admin/product/spuImageList/27
    //  http://api.gmall.com/admin/product/spuImageList/{spuId}
    @GetMapping("spuImageList/{spuId}")
    public Result getSpuImageList(@PathVariable Long spuId){
        //  需要调用服务层方法
        List<SpuImage> spuImageList = manageService.getSpuImageList(spuId);
        return Result.ok(spuImageList);
    }

    //  根据spuId 查询销售属性{ 销售属性值}
    //  http://api.gmall.com/admin/product/spuSaleAttrList/{spuId}
    @GetMapping("spuSaleAttrList/{spuId}")
    public Result getSpuSaleAttrList(@PathVariable Long spuId){
        //  调用服务层方法
        List<SpuSaleAttr> spuSaleAttrList = manageService.getSpuSaleAttrList(spuId);
        return Result.ok(spuSaleAttrList);
    }
}
