package com.atguigu.gmall.product.api;

import com.alibaba.fastjson.JSONObject;
import com.atguigu.gmall.common.result.Result;
import com.atguigu.gmall.model.product.*;
import com.atguigu.gmall.product.service.ManageService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

/**
 * @author atguigu-mqx
 */
@RestController
@RequestMapping("api/product")
public class ProductApiController {

    //  调用服务层方法
    @Autowired
    private ManageService manageService;

    //  这里的映射路径应该是给service-item 提供使用！
    @GetMapping("inner/getSkuInfo/{skuId}")
    public SkuInfo getSkuInfo(@PathVariable Long skuId){
        return manageService.getSkuInfo(skuId);
    }


    //  根据三级分类Id 获取分类的名称
    @GetMapping("inner/getCategoryView/{category3Id}")
    public BaseCategoryView getCategoryView(@PathVariable Long category3Id){
        return manageService.getBaseCategoryView(category3Id);
    }

    //  根据spuId,skuId 获取销售属性数据并锁定销售属性值！
    @GetMapping("inner/getSpuSaleAttrListCheckBySku/{skuId}/{spuId}")
    public List<SpuSaleAttr> getSpuSaleAttrListCheckBySku(@PathVariable Long skuId,
                                                          @PathVariable Long spuId){
            return manageService.getSpuSaleAttrListCheckBySku(skuId,spuId);
    }

    //  根据skuId 获取最新的商品价格：
    @GetMapping("inner/getSkuPrice/{skuId}")
    public BigDecimal getSkuPrice(@PathVariable Long skuId){
        return manageService.getSkuPrice(skuId);
    }

    //  根据spuId 获取到销售属性值Id 与skuId 组成的数据集
    @GetMapping("inner/getSkuValueIdsMap/{spuId}")
    public Map getSkuValueIdsMap(@PathVariable Long spuId){
        return manageService.getSkuValueIdsMap(spuId);
    }

    // 获取首页分类数据
    @GetMapping("getBaseCategoryList")
    public Result getBaseCategoryList(){
        //  调用服务层方法
        List<JSONObject> baseCategoryList = manageService.getBaseCategoryList();
        return Result.ok(baseCategoryList);
    }
    //  根据品牌Id 获取品牌数据
    @GetMapping("inner/getTrademark/{tmId}")
    public BaseTrademark getTrademark(@PathVariable Long tmId){
        return manageService.getTrademarkByTmId(tmId);
    }

    //  根据skuId 获取平台属性数据
    @GetMapping("inner/getAttrList/{skuId}")
    public List<BaseAttrInfo> getAttrList(@PathVariable Long skuId){
        return manageService.getAttrList(skuId);
    }

}
