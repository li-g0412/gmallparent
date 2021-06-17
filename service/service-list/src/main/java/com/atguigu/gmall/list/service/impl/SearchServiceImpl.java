package com.atguigu.gmall.list.service.impl;

import com.alibaba.fastjson.JSON;
import com.atguigu.gmall.list.repository.GoodsRepository;
import com.atguigu.gmall.list.service.SearchService;
import com.atguigu.gmall.model.list.*;
import com.atguigu.gmall.model.product.BaseAttrInfo;
import com.atguigu.gmall.model.product.BaseCategoryView;
import com.atguigu.gmall.model.product.BaseTrademark;
import com.atguigu.gmall.model.product.SkuInfo;
import com.atguigu.gmall.product.client.ProductFeignClient;
import org.apache.lucene.search.join.ScoreMode;
import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.common.text.Text;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.Operator;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.SearchHits;
import org.elasticsearch.search.aggregations.Aggregation;
import org.elasticsearch.search.aggregations.AggregationBuilders;
import org.elasticsearch.search.aggregations.bucket.nested.ParsedNested;
import org.elasticsearch.search.aggregations.bucket.terms.ParsedLongTerms;
import org.elasticsearch.search.aggregations.bucket.terms.ParsedStringTerms;
import org.elasticsearch.search.aggregations.bucket.terms.Terms;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.elasticsearch.search.fetch.subphase.highlight.HighlightBuilder;
import org.elasticsearch.search.sort.SortOrder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.io.IOException;
import java.util.*;
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

    @Autowired
    private RestHighLevelClient restHighLevelClient;

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

    @Override
    public SearchResponseVo search(SearchParam searchParam) throws IOException {
        /**
         * 1.先生成对用的dsl语句
         * 2.执行dsl语句
         * 3.将执行的结果集封装到SearchResponseVo 对象中
         */
        SearchRequest searchRequest = this.buildQueryDsl(searchParam);
        SearchResponse searchResponse = restHighLevelClient.search(searchRequest, RequestOptions.DEFAULT);
        SearchResponseVo searchResponseVo = this.parseSearchResult(searchResponse);
        searchResponseVo.setPageNo(searchParam.getPageNo());
        searchResponseVo.setPageSize(searchParam.getPageSize());
        Long totalpages = (searchResponseVo.getTotal()+searchParam.getPageSize()-1)/searchParam.getPageSize();

        searchResponseVo.setTotalPages(totalpages);

        return searchResponseVo;
    }

    /**
     * 返回的结果集
     * @param searchResponse
     * @return
     */
    private SearchResponseVo parseSearchResult(SearchResponse searchResponse) {
        SearchResponseVo searchResponseVo = new SearchResponseVo();
        //获取最外层的hits
        SearchHits hits = searchResponse.getHits();
        //获取内层的hits
        SearchHit[] subHits = hits.getHits();

        ArrayList<Goods> goodsList = new ArrayList<>();
        //循环
        for (SearchHit subHit : subHits) {
            //将Source对应的字符串转换为goods
            String sourceAsString = subHit.getSourceAsString();
            Goods goods = JSON.parseObject(sourceAsString, Goods.class);
            //细节处理 判断是否有高亮字段
            if (subHit.getHighlightFields().get("title") != null){
                //如果不为空取出里面的值
                Text title = subHit.getHighlightFields().get("title").getFragments()[0];
                goods.setTitle(title.toString());
            }
            //将goods添加到集合中
            goodsList.add(goods);
        }
        searchResponseVo.setGoodsList(goodsList);

        //品牌集合
        Map<String, Aggregation> aggregationMap = searchResponse.getAggregations().asMap();
        ParsedLongTerms tmIdAgg = (ParsedLongTerms) aggregationMap.get("tmIdAgg");
        List<SearchResponseTmVo> trademarkList = tmIdAgg.getBuckets().stream().map(bucket -> {
            SearchResponseTmVo tmVo = new SearchResponseTmVo();
            //给品牌赋值
            String tmId = ((Terms.Bucket)bucket).getKeyAsString();
            tmVo.setTmId(Long.parseLong(tmId));

            //tmName
            ParsedStringTerms tmNameAgg = bucket.getAggregations().get("tmNameAgg");
            String tmName = tmNameAgg.getBuckets().get(0).getKeyAsString();
            tmVo.setTmName(tmName);

            //tmLogoUrlAgg
            ParsedStringTerms tmLogoUrlAgg = bucket.getAggregations().get("tmLogoUrlAgg");
            String tmLogoUrl = tmLogoUrlAgg.getBuckets().get(0).getKeyAsString();
            tmVo.setTmName(tmLogoUrl);
            //返回品牌数据对象
            return tmVo;
        }).collect(Collectors.toList());

        searchResponseVo.setTrademarkList(trademarkList);

        //平台属性集合attrsList
        ParsedNested attrAgg = (ParsedNested) aggregationMap.get("attrAgg");
        ParsedLongTerms attrIdAgg = attrAgg.getAggregations().get("attrIdAgg");
        //获取桶中数据
        List<SearchResponseAttrVo> attrsList = attrIdAgg.getBuckets().stream().map(bucket -> {
            SearchResponseAttrVo searchResponseAttrVo = new SearchResponseAttrVo();
            //给AttrId赋值
            Number attrId = bucket.getKeyAsNumber();
            searchResponseAttrVo.setAttrId(attrId.longValue());
            //给attrName赋值
            ParsedStringTerms attrNameAgg = bucket.getAggregations().get("attrNameAgg");
            String attrName = attrNameAgg.getBuckets().get(0).getKeyAsString();
            searchResponseAttrVo.setAttrName(attrName);

            ParsedStringTerms attrValueAgg = bucket.getAggregations().get("attrValueAgg");
            List<String> attrValueList = attrValueAgg.getBuckets().stream().map(Terms.Bucket::getKeyAsString).collect(Collectors.toList());

            searchResponseAttrVo.setAttrValueList(attrValueList);
            return searchResponseAttrVo;
        }).collect(Collectors.toList());

        searchResponseVo.setAttrsList(attrsList);
        //获取到总条数
        searchResponseVo.setTotal(hits.getTotalHits().value);
        return searchResponseVo;
    }

    //生成dsl语句
    private SearchRequest buildQueryDsl(SearchParam searchParam) {
        //创建一个SearchSourceBuilder对象
        SearchSourceBuilder searchSourceBuilder = new SearchSourceBuilder();
        //创建一个bool对象
        BoolQueryBuilder boolQueryBuilder = QueryBuilders.boolQuery();
        //判断用户是否根据分类id检索
        if (!StringUtils.isEmpty(searchParam.getCategory1Id())){
            //创建一个filter
            boolQueryBuilder.filter(QueryBuilders.termQuery("category1Id",searchParam.getCategory1Id()));
        }
        if (!StringUtils.isEmpty(searchParam.getCategory2Id())){
            //创建一个filter
            boolQueryBuilder.filter(QueryBuilders.termQuery("category2Id",searchParam.getCategory2Id()));
        }
        if (!StringUtils.isEmpty(searchParam.getCategory3Id())){
            //创建一个filter
            boolQueryBuilder.filter(QueryBuilders.termQuery("category3Id",searchParam.getCategory3Id()));
        }

        //判断用户是否根据全文检索
        if (!StringUtils.isEmpty(searchParam.getKeyword())){
            boolQueryBuilder.must(QueryBuilders.matchQuery("title",searchParam.getKeyword()).operator(Operator.AND));
        }

        //获取到品牌数据
        String trademark = searchParam.getTrademark();
        if (!StringUtils.isEmpty(trademark)){
            //通过对字符串分割
            String[] split = trademark.split(":");
            //得到分割后的数组进行判断
            if (split!=null && split.length==2){
                //获取到品牌id
                boolQueryBuilder.filter(QueryBuilders.termQuery("tmId",split[0]));
            }
        }

        //通过平台属性值过滤
        String[] props = searchParam.getProps();
        if (props != null && props.length>0){
            //循环遍历当前数组
            for (String prop : props) {
                String[] split = prop.split(":");
                if (split != null && split.length == 3){
                    //声明两个bool对象
                    BoolQueryBuilder queryBuilder = QueryBuilders.boolQuery();
                    BoolQueryBuilder subQueryBuilder = QueryBuilders.boolQuery();
                    subQueryBuilder.must(QueryBuilders.termQuery("attrs.attrId",split[0]));
                    subQueryBuilder.must(QueryBuilders.termQuery("attrs.attrValue",split[1]));
                    //封装平台属性id 平台属性名称查询
                    queryBuilder.must(QueryBuilders.nestedQuery("attrs",subQueryBuilder, ScoreMode.None));
                    //将封装平台属性id 平台属性名称 赋值给外层的bool
                    boolQueryBuilder.filter(queryBuilder);
                }
            }
        }

        searchSourceBuilder.query(boolQueryBuilder);
        //设置高亮
        HighlightBuilder highlightBuilder = new HighlightBuilder();
        highlightBuilder.field("titles");
        highlightBuilder.preTags("<span style=color:red>");
        highlightBuilder.postTags("</span>");
        searchSourceBuilder.highlighter(highlightBuilder);

        //设置分页
        int from = (searchParam.getPageNo()-1)*searchParam.getPageSize();
        searchSourceBuilder.from(from);
        searchSourceBuilder.size(searchParam.getPageSize());

        //设置排序
        String order = searchParam.getOrder();
        if (StringUtils.isEmpty(order)){
            //进行分割
            String[] split = order.split(":");
            if (split != null && split.length == 2){
                //声明一个字段记录按照那种方式排序
                String field = "";
                //判断是按照那种方式排序
                switch (split[0]){
                    case "1":
                        field = "hotScore";
                        break;
                    case "2":
                        field = "price";
                        break;
                }
                //按照升序还是降序排序
                searchSourceBuilder.sort(field,"asc".equals(split[1])? SortOrder.ASC:SortOrder.DESC);
            }else {
                //默认排序
                searchSourceBuilder.sort("hotScore",SortOrder.DESC);
            }
        }

        //聚合:平台属性
        searchSourceBuilder.aggregation(AggregationBuilders.nested("attrAgg","attrs")
            .subAggregation(AggregationBuilders.terms("attrIdAgg").field("attrs.attrId")
            .subAggregation(AggregationBuilders.terms("attrNameAgg").field("attrs.attrName"))
                    .subAggregation(AggregationBuilders.terms("attrValueAgg").field("attrs.attrValue")))
        );

        //聚合:品牌
        searchSourceBuilder.aggregation(AggregationBuilders.terms("tmIdAgg").field("tmId")
                    .subAggregation(AggregationBuilders.terms("tmNameAgg").field("tmName"))
                    .subAggregation(AggregationBuilders.terms("tmLogoUrlAgg").field("tmLogoUrl")));


        //其它的设置
        searchSourceBuilder.fetchSource(new String[]{"id","defaultImg","title","price"},null);

        SearchRequest searchRequest = new SearchRequest("goods");
        String dsl = searchSourceBuilder.toString();
        System.out.println("dsl:\t"+dsl);
        searchRequest.source(searchSourceBuilder);

//        SearchSourceBuilder searchSourceBuilder = new SearchSourceBuilder();
//        searchSourceBuilder.query(QueryBuilders.matchAllQuery());
//        searchRequest.source(searchSourceBuilder);


        return searchRequest;
    }
}
