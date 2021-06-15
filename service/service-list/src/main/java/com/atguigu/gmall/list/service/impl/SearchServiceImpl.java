package com.atguigu.gmall.list.service.impl;

import com.atguigu.gmall.list.repository.GoodsRepository;
import com.atguigu.gmall.list.service.SearchService;
import com.atguigu.gmall.model.list.Goods;
import com.atguigu.gmall.model.list.SearchAttr;
import com.atguigu.gmall.model.product.BaseAttrInfo;
import com.atguigu.gmall.model.product.BaseCategoryView;
import com.atguigu.gmall.model.product.BaseTrademark;
import com.atguigu.gmall.model.product.SkuInfo;
import com.atguigu.gmall.product.client.ProductFeignClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

/**
 * @author atguigu-mqx
 */
@Service
public class SearchServiceImpl implements SearchService {

    //  服务层调用：product-client
    @Autowired
    private ProductFeignClient productFeignClient;

    //  需要获取到一个操作es 的客户端！
    //  ElasticsearchRestTemplate 底层使用高级客户端！
    @Autowired
    private GoodsRepository goodsRepository;

    @Autowired
    private RedisTemplate redisTemplate;

    @Override
    public void upperGoods(Long skuId) {

        //  通过productFeignClient 获取到数据，给Goods类！
        Goods goods = new Goods();

        CompletableFuture<SkuInfo> skuInfoCompletableFuture = CompletableFuture.supplyAsync(() -> {
            SkuInfo skuInfo = productFeignClient.getSkuInfo(skuId);
            //  Sku基本信息
            goods.setId(skuInfo.getId());
            goods.setDefaultImg(skuInfo.getSkuDefaultImg());
            goods.setPrice(skuInfo.getPrice().doubleValue());
            goods.setTitle(skuInfo.getSkuName());
            goods.setCreateTime(new Date());
            //  返回skuInfo
            return skuInfo;
        });

        //  Sku分类信息
        CompletableFuture<Void> categoryCompletableFuture = skuInfoCompletableFuture.thenAcceptAsync((skuInfo) -> {
            BaseCategoryView categoryView = productFeignClient.getCategoryView(skuInfo.getCategory3Id());
            goods.setCategory1Id(categoryView.getCategory1Id());
            goods.setCategory2Id(categoryView.getCategory2Id());
            goods.setCategory3Id(categoryView.getCategory3Id());
            goods.setCategory3Name(categoryView.getCategory3Name());
            goods.setCategory2Name(categoryView.getCategory2Name());
            goods.setCategory1Name(categoryView.getCategory1Name());
        });


        //  Sku的品牌信息
        CompletableFuture<Void> tmCompletableFuture = skuInfoCompletableFuture.thenAcceptAsync((skuInfo) -> {
            BaseTrademark trademark = productFeignClient.getTrademark(skuInfo.getTmId());
            goods.setTmId(trademark.getId());
            goods.setTmName(trademark.getTmName());
            goods.setTmLogoUrl(trademark.getLogoUrl());
        });


        //   Sku对应的平台属性
        CompletableFuture<Void> attrCompletableFuture = CompletableFuture.runAsync(() -> {
            List<BaseAttrInfo> attrList = productFeignClient.getAttrList(skuId);
            //  使用拉姆达表示
            List<SearchAttr> searchAttrList = attrList.stream().map((baseAttrInfo) -> {
                SearchAttr searchAttr = new SearchAttr();
                searchAttr.setAttrId(baseAttrInfo.getId());
                //  属性名称
                searchAttr.setAttrName(baseAttrInfo.getAttrName());
                //  属性值的名称
                String valueName = baseAttrInfo.getAttrValueList().get(0).getValueName();
                searchAttr.setAttrValue(valueName);

                return searchAttr;
            }).collect(Collectors.toList());

            goods.setAttrs(searchAttrList);
        });

        //  组合一下任务：
        CompletableFuture.allOf(skuInfoCompletableFuture,
                categoryCompletableFuture,
                tmCompletableFuture,
                attrCompletableFuture).join();


        //        Goods goods = new Goods();
        //        //  Sku基本信息
        //        SkuInfo skuInfo = productFeignClient.getSkuInfo(skuId);
        //        goods.setId(skuInfo.getId());
        //        goods.setDefaultImg(skuInfo.getSkuDefaultImg());
        //        goods.setPrice(skuInfo.getPrice().doubleValue());
        //        goods.setTitle(skuInfo.getSkuName());
        //        goods.setCreateTime(new Date());
        //        //  Sku分类信息
        //        BaseCategoryView categoryView = productFeignClient.getCategoryView(skuInfo.getCategory3Id());
        //
        //        goods.setCategory1Id(categoryView.getCategory1Id());
        //        goods.setCategory2Id(categoryView.getCategory2Id());
        //        goods.setCategory3Id(categoryView.getCategory3Id());
        //        goods.setCategory3Name(categoryView.getCategory3Name());
        //        goods.setCategory2Name(categoryView.getCategory2Name());
        //        goods.setCategory1Name(categoryView.getCategory1Name());
        //
        //
        //        //  Sku的品牌信息
        //        BaseTrademark trademark = productFeignClient.getTrademark(skuInfo.getTmId());
        //        goods.setTmId(trademark.getId());
        //        goods.setTmName(trademark.getTmName());
        //        goods.setTmLogoUrl(trademark.getLogoUrl());
        //
        //        //  Sku对应的平台属性
        //        List<BaseAttrInfo> attrList = productFeignClient.getAttrList(skuId);
        //
        //        //  创建一个
        //        List<SearchAttr> searchAttrList = new ArrayList<>();
        //        //  遍历集合
        //        for (BaseAttrInfo baseAttrInfo : attrList) {
        //            SearchAttr searchAttr = new SearchAttr();
        //            searchAttr.setAttrId(baseAttrInfo.getId());
        //            //  属性名称
        //            searchAttr.setAttrName(baseAttrInfo.getAttrName());
        //            //  属性值的名称
        //            String valueName = baseAttrInfo.getAttrValueList().get(0).getValueName();
        //            searchAttr.setAttrValue(valueName);
        //        }
        //        goods.setAttrs(searchAttrList);

        //  保存数据到es！上架
        this.goodsRepository.save(goods);
    }

    @Override
    public void lowerGoods(Long skuId) {
        //  删除
        this.goodsRepository.deleteById(skuId);
    }

    @Override
    public void incrHotScore(Long skuId) {
        //  借助redis 来实现！ 找一个数据类型来存储数据！ String 有这个功能！采用ZSet！
        String hotKey = "hotScore";
        //  ZSet 自增
        Double score = redisTemplate.opsForZSet().incrementScore(hotKey, "skuId:" + skuId, 1);
        //  判断
        if (score%10==0){
            //  更新es ！
            Optional<Goods> optional = this.goodsRepository.findById(skuId);
            Goods goods = optional.get();
            //  将最新的评分赋值给es！
            goods.setHotScore(score.longValue());
            //  将这个goods 保存到es
            this.goodsRepository.save(goods);
        }

    }
}
