package com.atguigu.gmall.product.service.impl;

import com.alibaba.fastjson.JSONObject;
import com.atguigu.gmall.common.cache.GmallCache;
import com.atguigu.gmall.common.constant.RedisConst;
import com.atguigu.gmall.model.product.*;
import com.atguigu.gmall.product.mapper.*;
import com.atguigu.gmall.product.service.ManageService;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.SneakyThrows;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;

import java.math.BigDecimal;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * @author atguigu-mqx
 */
@Service
public class ManageServiceImpl implements ManageService {
    //  调用mapper层
    @Autowired
    private BaseCategory1Mapper baseCategory1Mapper;

    @Autowired
    private BaseCategory2Mapper baseCategory2Mapper;

    @Autowired
    private BaseCategory3Mapper baseCategory3Mapper;

    @Autowired
    private BaseAttrInfoMapper baseAttrInfoMapper;

    @Autowired
    private BaseAttrValueMapper baseAttrValueMapper;

    @Autowired
    private SpuInfoMapper spuInfoMapper;

    @Autowired
    private BaseSaleAttrMapper baseSaleAttrMapper;

    @Autowired
    private SpuImageMapper spuImageMapper;

    @Autowired
    private SpuSaleAttrMapper spuSaleAttrMapper;

    @Autowired
    private SpuSaleAttrValueMapper spuSaleAttrValueMapper;

    @Autowired
    private SkuInfoMapper skuInfoMapper;

    @Autowired
    private SkuImageMapper skuImageMapper;

    @Autowired
    private SkuSaleAttrValueMapper skuSaleAttrValueMapper;

    @Autowired
    private SkuAttrValueMapper skuAttrValueMapper;

    @Autowired
    private BaseCategoryViewMapper baseCategoryViewMapper;

    @Autowired
    private RedisTemplate redisTemplate;

    @Autowired
    private RedissonClient redissonClient;

    @Autowired
    private BaseTrademarkMapper baseTrademarkMapper;

    @Override
    public List<BaseCategory1> getBaseCategory1() {
        //  select * from base_category1
        return baseCategory1Mapper.selectList(null);
    }

    @Override
    public List<BaseCategory2> getBaseCategory2(Long category1Id) {
        //  select * from base_category2 where category1_id = ?
        return baseCategory2Mapper.selectList(new QueryWrapper<BaseCategory2>().eq("category1_id",category1Id));
    }

    @Override
    public List<BaseCategory3> getBaseCategory3(Long category2Id) {
        //  select * from base_category3 where category2_id = ?
        return baseCategory3Mapper.selectList(new QueryWrapper<BaseCategory3>().eq("category2_id",category2Id));
    }

    @Override
    public List<BaseAttrInfo> getBaseAttrInfoList(Long category1Id, Long category2Id, Long category3Id) {
        //  select * from base_attr_info where category_id = category1Id and category_level = 1
        //  select * from base_attr_info where category_id = category2Id and category_level = 2
        //  select * from base_attr_info where category_id = category3Id and category_level = 3
        //  在此引出mybatis 中的动态sql标签！
        //  后续的功能中，有个需求：根据分类Id ，查询平台属性，平台属性值：
        List<BaseAttrInfo> baseAttrInfoList = baseAttrInfoMapper.selectBaseAttrInfoList(category1Id,category2Id,category3Id);
        //  返回数据
        return baseAttrInfoList;
    }

    //  该方法既有对平台属性的新增，也有对平台属性的修改！
    @Override
    @Transactional(rollbackFor = Exception.class)   //  在方法体内发生异常的时候，就会回滚！
    public void saveAttrInfo(BaseAttrInfo baseAttrInfo) {
        //  base_attr_info
        if (baseAttrInfo.getId()!=null){
            //  修改操作
            baseAttrInfoMapper.updateById(baseAttrInfo);
        }else {
            baseAttrInfoMapper.insert(baseAttrInfo);
        }

        //  base_attr_value int i = 1/0; 先删除平台属性Id 对应的属性值数据，再做新增
        //  delete from base_attr_value where attr_id = ?
        QueryWrapper<BaseAttrValue> baseAttrValueQueryWrapper = new QueryWrapper<>();
        baseAttrValueQueryWrapper.eq("attr_id",baseAttrInfo.getId());
        baseAttrValueMapper.delete(baseAttrValueQueryWrapper);

        //  必须获取到平台属性值数据
        List<BaseAttrValue> attrValueList = baseAttrInfo.getAttrValueList();
        if (!CollectionUtils.isEmpty(attrValueList)){
            //  循环遍历添加
            for (BaseAttrValue baseAttrValue : attrValueList) {
                //  base_attr_value.attr_id = base_attr_info.id
                baseAttrValue.setAttrId(baseAttrInfo.getId());
                baseAttrValueMapper.insert(baseAttrValue);
            }
        }
    }

    @Override
    public List<BaseAttrValue> getAttrValueList(Long attrId) {
        //  select * from base_attr_value where attr_id = ?
        List<BaseAttrValue> baseAttrValueList = baseAttrValueMapper.selectList(new QueryWrapper<BaseAttrValue>().eq("attr_id", attrId));
        //  返回数据
        return baseAttrValueList;
    }

    @Override
    public BaseAttrInfo getBaseAttrInfo(Long attrId) {
        //  根据主键查询平台属性对象
        //  select * from base_attr_info where id = attrId;
        BaseAttrInfo baseAttrInfo = baseAttrInfoMapper.selectById(attrId);
        //  判断平台属性是否为空
        if (baseAttrInfo!=null){
            //  select * from base_attr_value where attr_id = ?
            baseAttrInfo.setAttrValueList(getAttrValueList(attrId));
        }
        //  返回平台属性对象
        return baseAttrInfo;
    }

    @Override
    public IPage<SpuInfo> getPage(Page<SpuInfo> page, Long category3Id) {
        //  封装条件
        QueryWrapper<SpuInfo> spuInfoQueryWrapper = new QueryWrapper<>();
        spuInfoQueryWrapper.eq("category3_id",category3Id);
        spuInfoQueryWrapper.orderByDesc("id");
        //  需要两个参数，一个是page 当前页，每页显示的条数，第二个Wrapper 条件
        Page<SpuInfo> spuInfoPage = spuInfoMapper.selectPage(page, spuInfoQueryWrapper);
        return spuInfoPage;
    }

    @Override
    public IPage<SpuInfo> getPages(Page<SpuInfo> page,SpuInfo spuInfo) {
        //  封装条件
        QueryWrapper<SpuInfo> spuInfoQueryWrapper = new QueryWrapper<>();
        spuInfoQueryWrapper.eq("category3_id",spuInfo.getCategory3Id());
        spuInfoQueryWrapper.orderByDesc("id");
        //  需要两个参数，一个是page 当前页，每页显示的条数，第二个Wrapper 条件
        Page<SpuInfo> spuInfoPage = spuInfoMapper.selectPage(page, spuInfoQueryWrapper);
        return spuInfoPage;
    }

    @Override
    public List<BaseSaleAttr> getBseSaleAttrList() {
        //  select * from base_sale_attr
        List<BaseSaleAttr> baseSaleAttrList = baseSaleAttrMapper.selectList(null);

        return baseSaleAttrList;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void saveSpuInfo(SpuInfo spuInfo) {
        /*
            spuInfo;
            spuImage;
            spuSaleAttr;
            spuSaleAttrValue;
         */
        spuInfoMapper.insert(spuInfo);

        //  获取到spuImage 集合数据
        List<SpuImage> spuImageList = spuInfo.getSpuImageList();
        //  判断不为空
        if (!CollectionUtils.isEmpty(spuImageList)){
            //  循环遍历
            for (SpuImage spuImage : spuImageList) {
                //  需要将spuId 赋值
                spuImage.setSpuId(spuInfo.getId());
                //  保存spuImge
                spuImageMapper.insert(spuImage);
            }
        }
        //  获取销售属性集合
        List<SpuSaleAttr> spuSaleAttrList = spuInfo.getSpuSaleAttrList();
        //  判断
        if (!CollectionUtils.isEmpty(spuSaleAttrList)){
            //  循环遍历
            for (SpuSaleAttr spuSaleAttr : spuSaleAttrList) {
                //  需要将spuId 赋值
                spuSaleAttr.setSpuId(spuInfo.getId());
                spuSaleAttrMapper.insert(spuSaleAttr);

                //  再此获取销售属性值集合
                List<SpuSaleAttrValue> spuSaleAttrValueList = spuSaleAttr.getSpuSaleAttrValueList();
                //  判断
                if (!CollectionUtils.isEmpty(spuSaleAttrValueList)){
                    //  循环遍历
                    for (SpuSaleAttrValue spuSaleAttrValue : spuSaleAttrValueList) {
                        //   需要将spuId， sale_attr_name 赋值
                        spuSaleAttrValue.setSpuId(spuInfo.getId());
                        spuSaleAttrValue.setSaleAttrName(spuSaleAttr.getSaleAttrName());
                        spuSaleAttrValueMapper.insert(spuSaleAttrValue);
                    }
                }
            }
        }
    }

    @Override
    public List<SpuImage> getSpuImageList(Long spuId) {
        //  select * from spu_image where spu_id = ?
        QueryWrapper<SpuImage> spuImageQueryWrapper = new QueryWrapper<>();
        spuImageQueryWrapper.eq("spu_id",spuId);
        return spuImageMapper.selectList(spuImageQueryWrapper);

    }

    @Override
    public List<SpuSaleAttr> getSpuSaleAttrList(Long spuId) {
        //  不仅需要获取到销售属性名，还需要获取到销售属性值名称！ spu_sale_attr ， spu_sale_attr_value
        List<SpuSaleAttr>  spuSaleAttrList = spuSaleAttrMapper.selectSpuSaleAttrList(spuId);
        return spuSaleAttrList;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void saveSkuInfo(SkuInfo skuInfo) {
        /*
        skuInfo
        skuAttrValue
        skuSaleAttrValue
        skuImage
         */
        //  保存skuInfo
        skuInfoMapper.insert(skuInfo);
        //  skuAttrValue
        //  获取数据
        List<SkuAttrValue> skuAttrValueList = skuInfo.getSkuAttrValueList();
        //  判断
        if (!CollectionUtils.isEmpty(skuAttrValueList)){
            //  循环遍历
            for (SkuAttrValue skuAttrValue : skuAttrValueList) {
                //  给skuId 赋值
                skuAttrValue.setSkuId(skuInfo.getId());
                skuAttrValueMapper.insert(skuAttrValue);
            }
        }

        //  先获取到对应的数据
        List<SkuSaleAttrValue> skuSaleAttrValueList = skuInfo.getSkuSaleAttrValueList();
        if (!CollectionUtils.isEmpty(skuSaleAttrValueList)){
            //  循环遍历
            for (SkuSaleAttrValue skuSaleAttrValue : skuSaleAttrValueList) {
                //  sku_id
                skuSaleAttrValue.setSkuId(skuInfo.getId());
                //  spu_id
                skuSaleAttrValue.setSpuId(skuInfo.getSpuId());
                skuSaleAttrValueMapper.insert(skuSaleAttrValue);
            }
        }
        //   skuImage
        List<SkuImage> skuImageList = skuInfo.getSkuImageList();
        if (!CollectionUtils.isEmpty(skuImageList)){
            //  循环遍历
            for (SkuImage skuImage : skuImageList) {
                //  sku_id
                skuImage.setSkuId(skuInfo.getId());
                skuImageMapper.insert(skuImage);
            }
        }
    }

    @Override
    public IPage<SkuInfo> getSkuInfoList(Page<SkuInfo> skuInfoPage) {
        //  查询的时候按照id 进行降序排列
        QueryWrapper<SkuInfo> skuInfoQueryWrapper = new QueryWrapper<>();
        skuInfoQueryWrapper.orderByDesc("id");
        return skuInfoMapper.selectPage(skuInfoPage,skuInfoQueryWrapper);
    }

    @Override
    public void onSale(Long skuId) {
        //  本质： is_sale = 1
        //  update sku_info set is_sale = 1 where id = skuId
        //  需要的是实体类：实体类中一定得有主键！

        //        SkuInfo skuInfo = skuInfoMapper.selectById(skuId);
        //        skuInfo.setIsSale(1);
        //        skuInfoMapper.updateById(skuInfo);
        SkuInfo skuInfo = new SkuInfo();
        skuInfo.setId(skuId);
        skuInfo.setIsSale(1);
        skuInfoMapper.updateById(skuInfo);


    }

    @Override
    public void cancelSale(Long skuId) {
        //  本质： is_sale = 0
        //  update sku_info set is_sale = 0 where id = skuId
        SkuInfo skuInfo = new SkuInfo();
        skuInfo.setId(skuId);
        skuInfo.setIsSale(0);
        skuInfoMapper.updateById(skuInfo);
    }

    @SneakyThrows
    @Override
    @GmallCache(prefix = "skuInfo:")
    public SkuInfo getSkuInfo(Long skuId) {

        /*
        if(true){
            //  getReids();
        }else{
            //  getSkuInfoDB(skuId);
            //  将结果放入缓存！
        }
         */
        //  return getSkuInfoByRedis(skuId);
        //  return getSkuInfoByRedisson(skuId);
        return getSkuInfoDB(skuId);
    }

    private SkuInfo getSkuInfoByRedisson(Long skuId) {
        SkuInfo skuInfo = null;
        try {
            skuInfo = null;
            //  skuInfo 数据key 这个key 要保证唯一！
            //  skuKey = sku:skuId:info
            String skuKey = RedisConst.SKUKEY_PREFIX+skuId+RedisConst.SKUKEY_SUFFIX;
            //  通过key 获取缓存中的数据
            //  set sku:skuId:info skuInfo
            skuInfo = (SkuInfo) redisTemplate.opsForValue().get(skuKey);
            //  判断缓存中是否有数据
            if (skuInfo==null){
                //  直接调用redisson 的方法就可以了。
                String lockKey = RedisConst.SKUKEY_PREFIX+skuId+RedisConst.SKULOCK_SUFFIX;
                RLock lock = redissonClient.getLock(lockKey);
                //  上锁
                boolean result = lock.tryLock(RedisConst.SKULOCK_EXPIRE_PX1, RedisConst.SKULOCK_EXPIRE_PX2, TimeUnit.SECONDS);
                //  判断是否获取到锁！
                if (result){
                    try {
                        //  业务逻辑代码
                        skuInfo = getSkuInfoDB(skuId);
                        //  判断skuInfo
                        if (skuInfo==null){
                            //  创建一个空对象
                            SkuInfo skuInfo1 = new SkuInfo();
                            //  放入缓存
                            redisTemplate.opsForValue().set(skuKey,skuInfo1,RedisConst.SKUKEY_TEMPORARY_TIMEOUT,TimeUnit.SECONDS);
                            //  返回当前数据
                            return skuInfo1;
                        }
                        //  放入缓存
                        redisTemplate.opsForValue().set(skuKey,skuInfo,RedisConst.SKUKEY_TIMEOUT,TimeUnit.SECONDS);
                        //  返回数据
                        //  lockWatchdogTimeout
                        return skuInfo;
                    }finally {
                        lock.unlock();
                    }
                }else {
                    //  没有获取到分布式锁！
                    //  睡一会，自旋
                    try {
                        Thread.sleep(500);
                        return getSkuInfo(skuId);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            }else {
                //  返回缓存的数据！
                return skuInfo;
            }
        } catch (InterruptedException e) {
            //  记录日志，发送短信通知
            e.printStackTrace();
        }

        //  返回数据！
        return getSkuInfoDB(skuId);
    }

    /**
     * 使用redis+lua 脚本做分布式锁！
     * @param skuId
     * @return
     */
    private SkuInfo getSkuInfoByRedis(Long skuId) {
        SkuInfo skuInfo = null;
        try {
            //  skuInfo 数据key 这个key 要保证唯一！
            //  skuKey = sku:skuId:info
            String skuKey = RedisConst.SKUKEY_PREFIX+skuId+RedisConst.SKUKEY_SUFFIX;
            //  通过key 获取缓存中的数据
            //  set sku:skuId:info skuInfo
            skuInfo = (SkuInfo) redisTemplate.opsForValue().get(skuKey);

            //  判断
            if (skuInfo==null){
                //  缓存中没有数据，从数据库获取，放入缓存！但是，有可能会造成缓存击穿，穿透！
                //  skuId = 47 击穿 ,skuId = 147 不能 穿透
                //  定义一个锁：lockKey = sku:skuId:lock
                String lockKey = RedisConst.SKUKEY_PREFIX+skuId+RedisConst.SKULOCK_SUFFIX;
                String uuid = UUID.randomUUID().toString();
                //  使用命令能否获取到锁！
                Boolean flag = redisTemplate.opsForValue().setIfAbsent(lockKey, uuid, RedisConst.SKULOCK_EXPIRE_PX1, TimeUnit.SECONDS);
                //  判断是否获取到锁
                if (flag){
                    //  获取到锁：
                    skuInfo = getSkuInfoDB(skuId);
                    //  判断skuInfo
                    if (skuInfo==null){
                        //  创建一个空对象
                        SkuInfo skuInfo1 = new SkuInfo();
                        //  放入缓存
                        redisTemplate.opsForValue().set(skuKey,skuInfo1,RedisConst.SKUKEY_TEMPORARY_TIMEOUT,TimeUnit.SECONDS);
                        //  返回当前数据
                        return skuInfo1;
                    }
                    //  放入缓存
                    redisTemplate.opsForValue().set(skuKey,skuInfo,RedisConst.SKUKEY_TIMEOUT,TimeUnit.SECONDS);

                    //  释放锁！建议使用lua 脚本  可以将删除锁的操作放入finally！
                    //  第一个参数； RedisScript
                    DefaultRedisScript<Long> redisScript = new DefaultRedisScript<>();
                    //  使用lua 脚本：
                    String script = "if redis.call('get', KEYS[1]) == ARGV[1] then return redis.call('del', KEYS[1]) else return 0 end";
                    //  放入lua 脚本！
                    redisScript.setScriptText(script);
                    //  设置返回的类型
                    redisScript.setResultType(Long.class);
                    // 第一个参数； RedisScript 第二个参数应该是key ，第三个参数应该是value！
                    redisTemplate.execute(redisScript, Arrays.asList(lockKey),uuid);

                    //  返回数据库中的数据
                    return skuInfo;
                }else {
                    //  没有获取到分布式锁！
                    //  睡一会，自旋
                    try {
                        Thread.sleep(500);
                        return getSkuInfo(skuId);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            }else {
                //  直接返回即可！
                return skuInfo;
            }
        } catch (Exception e) {
            //  记录日志，当前哪台机器宕机了。。。。。。调用一个发送短信接口！
            e.printStackTrace();
        }
        //  返回数据 ， 暂时可以采用数据库兜底！
        return getSkuInfoDB(skuId);
    }

    /**
     * 抽离方法 通过skuId 获取skuInfo + skuIamgeList
     * @param skuId
     * @return
     */
    private SkuInfo getSkuInfoDB(Long skuId) {
        //  select * from sku_info where id = skuId;
        SkuInfo skuInfo = skuInfoMapper.selectById(skuId);

        if (skuInfo!=null){
            //  可以将skuImageList 查询出来放入skuInfo 对象！
            //  select * from sku_image where sku_id = skuId;
            List<SkuImage> skuImageList = skuImageMapper.selectList(new QueryWrapper<SkuImage>().eq("sku_id", skuId));

            //  给当前的skuInfo 属性赋值
            skuInfo.setSkuImageList(skuImageList);
        }
        //  返回数据
        return skuInfo;
    }


    @Override
    @GmallCache(prefix = "BaseCategoryView:")
    public BaseCategoryView getBaseCategoryView(Long category3Id) {
        //  select * from base_category_view where id = 61;
        return baseCategoryViewMapper.selectById(category3Id);
    }

    @Override
    @GmallCache(prefix = "getSpuSaleAttrListCheckBySku:")
    public List<SpuSaleAttr> getSpuSaleAttrListCheckBySku(Long skuId, Long spuId) {
        //  调用mapper 的方法
        /*
        select
            ssa.id,
            ssa.spu_id,
            ssa.base_sale_attr_id,
            ssa.sale_attr_name,
            ssav.id sale_attr_value_id,
            ssav.sale_attr_value_name,
            if(sav.sku_id is null,0,1) is_checked #
        from spu_sale_attr ssa inner join spu_sale_attr_value  ssav
            on  ssa.spu_id = ssav.spu_id and ssa.base_sale_attr_id = ssav.base_sale_attr_id
            left join sku_sale_attr_value sav on sav.sale_attr_value_id = ssav.id and sku_id = 44
        where ssa.spu_id = 27
        order by ssa.base_sale_attr_id,ssav.id;
         */
        return spuSaleAttrMapper.selectSpuSaleAttrListCheckBySku(skuId,spuId);
    }

    @Override
    //  @GmallCache(prefix = "price:")
    public BigDecimal getSkuPrice(Long skuId) {
        //  select price from sku_info where id = skuId;
        SkuInfo skuInfo = skuInfoMapper.selectById(skuId);
        //  select * from sku_info where id = skuId;
        if (skuInfo!=null){
            return skuInfo.getPrice();
        }else {
            return new BigDecimal(0);
        }
    }

    @Override
    @GmallCache(prefix = "getSkuValueIdsMap:")
    public Map getSkuValueIdsMap(Long spuId) {
        Map<Object,Object> hashMap = new HashMap<>();
        /*
            第一种方式：
            public class Param{
                private Long skuId;
                private String valueIds;
            }
            List<Param> paramList = mapper.selectxxx(spuId);
            第二种方式：
                直接定义成Map 集合
                map.put(key,value);

            List<Map> mapList =  mapper.selectxxx(spuId);
         */
        //  使用哪个mapper 去执行?
        List<Map> mapList = skuSaleAttrValueMapper.selectSaleAttrValuesBySpu(spuId);
        for (Map map : mapList) {
            //  hashMap 转换成 {"115|117":"44","114|117":"45"}
            //  key = 115|117  value = 44
            hashMap.put(map.get("value_ids"),map.get("sku_id"));
        }
        //  返回数据
        return hashMap;
    }

    @Override
    @GmallCache(prefix = "getBaseCategoryList:")
    public List<JSONObject> getBaseCategoryList() {
        List<JSONObject> list = new ArrayList<>();
        //        JSONObject jsonObject = new JSONObject();
        //        //  后台数据格式：json key  - value
        //        jsonObject.put("index",1); // 底层调用就是Map
        //  先获取到所有的分类数据集合 baseCategory1  baseCategory2 baseCategory3 base_category_view
        List<BaseCategoryView> baseCategoryViewList = baseCategoryViewMapper.selectList(null);

        //  找到那些是一级分类数据 以一级分类Id 进行分组
        //  map  key  = category1Id value = List<BaseCategoryView>
        Map<Long, List<BaseCategoryView>> category1Map = baseCategoryViewList.stream().collect(Collectors.groupingBy(BaseCategoryView::getCategory1Id));

        //  定义一个index
        int index = 1;
        //  循环遍历map
        for (Map.Entry<Long, List<BaseCategoryView>> entry1 : category1Map.entrySet()) {
            //  获取一级分类Id
            Long category1Id = entry1.getKey();
            //  获取一级分类Id 下的所有集合数据
            List<BaseCategoryView> category2List = entry1.getValue();
            //  声明一个一级分类对象
            JSONObject category1 = new JSONObject();
            category1.put("index",index);
            category1.put("categoryId",category1Id);
            category1.put("categoryName",category2List.get(0).getCategory1Name());

            //  index 迭代
            index++;
            //  哪些是二级分类数据？  以二级分类Id 进行分组
            //  key = category2Id ，value =  List<BaseCategoryView>
            Map<Long, List<BaseCategoryView>> category2Map = category2List.stream().collect(Collectors.groupingBy(BaseCategoryView::getCategory2Id));

            //  声明一个集合来存储对应的二级分类对象！
            List<JSONObject> category2Child = new ArrayList<>();
            //  循环遍历
            for (Map.Entry<Long, List<BaseCategoryView>> entry2 : category2Map.entrySet()) {
                //  二级分类Id category2Id
                Long category2Id = entry2.getKey();
                //  二级分类Id 对应的所有的集合数据
                List<BaseCategoryView> category3List = entry2.getValue();

                //  声明一个二级分类对象
                JSONObject category2 = new JSONObject();
                category2.put("categoryId",category2Id);
                category2.put("categoryName",category3List.get(0).getCategory2Name());

                //  需要将每个二级分类添加到集合中！
                category2Child.add(category2);

                //  声明一个集合来存储对应的三级分类对象！
                List<JSONObject> category3Child = new ArrayList<>();
                //  哪些是三级分类数据?
                category3List.forEach((baseCategory3View)->{
                    //  创建一个三级分类对象
                    JSONObject category3 = new JSONObject();
                    category3.put("categoryId",baseCategory3View.getCategory3Id());
                    category3.put("categoryName",baseCategory3View.getCategory3Name());
                    //  将三级分类对象添加到集合
                    category3Child.add(category3);
                });
                category2.put("categoryChild",category3Child);
            }
            //  将二级分类数据放入一级分类数据中！
            category1.put("categoryChild",category2Child);
            //  将当前的一级分类数据放入list 集合
            list.add(category1);
        }
        return list;
    }

    @Override
    public BaseTrademark getTrademarkByTmId(Long tmId) {
        //  select * from base_trademark where id = tmId
        BaseTrademark baseTrademark = baseTrademarkMapper.selectById(tmId);
        return baseTrademark;
    }

    @Override
    public List<BaseAttrInfo> getAttrList(Long skuId) {
        //  根据skuId 获取平台属性数据集合
        List<BaseAttrInfo> baseAttrInfoList =  baseAttrInfoMapper.selectAttrList(skuId);
        return baseAttrInfoList;
    }
}
