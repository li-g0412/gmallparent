package com.atguigu.gmall.item.service.impl;

import com.alibaba.fastjson.JSON;
import com.atguigu.gmall.item.service.ItemService;
import com.atguigu.gmall.list.client.ListFeignClient;
import com.atguigu.gmall.model.product.BaseCategoryView;
import com.atguigu.gmall.model.product.SkuInfo;
import com.atguigu.gmall.model.product.SpuSaleAttr;
import com.atguigu.gmall.product.client.ProductFeignClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ThreadPoolExecutor;

/**
 * @author atguigu-mqx
 */
@Service
public class ItemServiceImpl implements ItemService {

    //  做数据汇总：service-product 这个微服务！
    //  需要使用远程调用：
    @Autowired
    private ProductFeignClient productFeignClient;

    @Autowired
    private ListFeignClient listFeignClient;

    @Autowired
    private ThreadPoolExecutor threadPoolExecutor;


    @Override
    public Map<String, Object> getItem(Long skuId) {

        Map<String, Object> map = new HashMap<>();
        /*
        1.	查询sku的基本信息
        2.	查询skuImage
        3.	分类信息
        4.	spu销售属性，属性值
        5.	查询最新价格
        6.  实现切换功能，需要后台获取到Json 字符串！
         */
        CompletableFuture<SkuInfo> skuInfoCompletableFuture = CompletableFuture.supplyAsync(() -> {
            //  根据skuId 获取skuInfo 数据并放入map 中！
            SkuInfo skuInfo = productFeignClient.getSkuInfo(skuId);
            map.put("skuInfo",skuInfo);
            return skuInfo;
        },threadPoolExecutor);

        //  根据分类Id 查询分类数据
        CompletableFuture<Void> categoryViewCompletableFuture = skuInfoCompletableFuture.thenAcceptAsync((skuInfo) -> {
            //  分类信息
            BaseCategoryView categoryView = productFeignClient.getCategoryView(skuInfo.getCategory3Id());
            map.put("categoryView", categoryView);
        },threadPoolExecutor);

        //  获取销售属性
        CompletableFuture<Void> spuSaleAttrListCompletableFuture = skuInfoCompletableFuture.thenAcceptAsync(skuInfo -> {
            //  spu销售属性，属性值
            List<SpuSaleAttr> spuSaleAttrList = productFeignClient.getSpuSaleAttrListCheckBySku(skuId, skuInfo.getSpuId());
            map.put("spuSaleAttrList", spuSaleAttrList);
        },threadPoolExecutor);

        //  切换数据
        CompletableFuture<Void> valuesSkuJsonCompletableFuture = skuInfoCompletableFuture.thenAcceptAsync((skuInfo) -> {
            Map maps = productFeignClient.getSkuValueIdsMap(skuInfo.getSpuId());
            //  将maps 转换为json 数据 {"115|117":"44","114|117":"45"}
            String mapJson = JSON.toJSONString(maps);
            map.put("valuesSkuJson", mapJson);
        },threadPoolExecutor);

        //  获取最新价格：
        CompletableFuture<Void> priceCompletableFuture = CompletableFuture.runAsync(() -> {
            //  查询最新价格
            BigDecimal skuPrice = productFeignClient.getSkuPrice(skuId);
            map.put("price", skuPrice);
        },threadPoolExecutor);


        //  调用热度排名数据接口：
        CompletableFuture<Void> hostCompletableFuture = CompletableFuture.runAsync(() -> {
           listFeignClient.incrHotScore(skuId);
        },threadPoolExecutor);
        //  多任务组合
        CompletableFuture.allOf(skuInfoCompletableFuture,
                                categoryViewCompletableFuture,
                                spuSaleAttrListCompletableFuture,
                                priceCompletableFuture,
                                valuesSkuJsonCompletableFuture,
                                hostCompletableFuture).join();

        //  返回数据
        return map;
    }
}
