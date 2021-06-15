package com.atguigu.gmall.product.service;

import com.alibaba.fastjson.JSONObject;
import com.atguigu.gmall.model.product.*;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

/**
 * @author atguigu-mqx
 */
public interface ManageService {

    /**
     * 查询所有一级分类数据
     * @return
     */
    List<BaseCategory1> getBaseCategory1();

    /**
     * 根据一级分类Id 获取二级分类数据
     * @param category1Id
     * @return
     */
    List<BaseCategory2> getBaseCategory2(Long category1Id);

    /**
     * 根据二级分类Id 查询三级分类数据
     * @param category2Id
     * @return
     */
    List<BaseCategory3> getBaseCategory3(Long category2Id);

    /**
     * 根据一级分类Id，二级分类Id，三级分类id 查询平台属性列表！
     * @param category1Id
     * @param category2Id
     * @param category3Id
     * @return
     */
    List<BaseAttrInfo> getBaseAttrInfoList(Long category1Id,Long category2Id,Long category3Id);

    /**
     * 保存平台属性数据
     * @param baseAttrInfo
     */
    void saveAttrInfo(BaseAttrInfo baseAttrInfo);

    /**
     * 根据平台属性Id 获取到平台属性值集合数据
     * @param attrId
     * @return
     */
    List<BaseAttrValue> getAttrValueList(Long attrId);

    /**
     * 通过平台属性Id 获取到平台属性对象
     * @param attrId
     * @return
     */
    BaseAttrInfo getBaseAttrInfo(Long attrId);

    /**
     * 根据三级分类Id 查询spuInfo列表
     * @param category3Id
     * @return
     */
    IPage<SpuInfo> getPage(Page<SpuInfo> page, Long category3Id);

    /**
     * 根据三级分类Id 查询spuInfo列表
     * @param spuInfo
     * @return
     */
    IPage<SpuInfo> getPages(Page<SpuInfo> page,SpuInfo spuInfo);

    /**
     * 获取所有的销售属性列表
     * @return
     */
    List<BaseSaleAttr> getBseSaleAttrList();

    /**
     * 保存spuInfo 数据
     * @param spuInfo
     */
    void saveSpuInfo(SpuInfo spuInfo);

    /**
     * 根据spuId 查询spuImgaeList
     * @param spuId
     * @return
     */
    List<SpuImage> getSpuImageList(Long spuId);

    /**
     * 根据spuId 获取对应的销售属性集合
     * @param spuId
     * @return
     */
    List<SpuSaleAttr> getSpuSaleAttrList(Long spuId);

    /**
     * 保存skuInfo 数据
     * @param skuInfo
     */
    void saveSkuInfo(SkuInfo skuInfo);

    /**
     * 查询带分页功能的skuInfo 列表
     * @param skuInfoPage
     * @return
     */
    IPage<SkuInfo> getSkuInfoList(Page<SkuInfo> skuInfoPage);

    /**
     * 商品的上架功能
     * @param skuId
     */
    void onSale(Long skuId);

    /**
     * 商品的下架功能
     * @param skuId
     */
    void cancelSale(Long skuId);

    /**
     * 根据skuId 获取skuInfo 对象数据
     * @param skuId
     * @return
     */
    SkuInfo getSkuInfo(Long skuId);

    /**
     * 根据三级分类Id 获取分类数据
     * @param category3Id
     * @return
     */
    BaseCategoryView getBaseCategoryView(Long category3Id);

    /**
     * 根据spuId,skuId 获取销售属性数据并锁定销售属性值！
     * @param skuId
     * @param spuId
     * @return
     */
    List<SpuSaleAttr> getSpuSaleAttrListCheckBySku(Long skuId, Long spuId);

    /**
     * 根据skuId 获取到最新价格数据
     * @param skuId
     * @return
     */
    BigDecimal getSkuPrice(Long skuId);

    /**
     * 根据spuId 获取销售属性值与skuId 组成的map 集合！
     * @param spuId
     * @return
     */
    Map getSkuValueIdsMap(Long spuId);

    //  获取到分类数据：
    List<JSONObject> getBaseCategoryList();

    /**
     * 通过品牌Id 来查询数据
     * @param tmId
     * @return
     */
    BaseTrademark getTrademarkByTmId(Long tmId);

    /**
     * 通过skuId 集合来查询数据
     * @param skuId
     * @return
     */
    List<BaseAttrInfo> getAttrList(Long skuId);


}
