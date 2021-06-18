package com.atguigu.gmall.all.controller;

import com.atguigu.gmall.common.result.Result;
import com.atguigu.gmall.list.client.ListFeignClient;
import com.atguigu.gmall.model.list.SearchParam;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author atguigu-mqx
 */
@Controller
public class ListController {

    @Autowired
    private ListFeignClient listFeignClient;

    //  http://list.gmall.com/list.html?category3Id = 61
    @GetMapping("list.html")
    public String getList(SearchParam searchParam, Model model){

        //  调用list微服务方法
        Result<Map> result = listFeignClient.list(searchParam);
        //  需要存储一个 urlParam，searchParam ，trademarkParam ，propsParamList ，trademarkList，attrsList，goodsList，orderMap 对象！
        //  SearchResponseVo s = result.getData();
        //  实体类与map 可以互相代替！ trademarkList，attrsList，goodsList
        model.addAllAttributes(result.getData());
        //  searchParam
        model.addAttribute("searchParam",searchParam);
        //  考虑urlParam 是什么? 记录用户的检索条件 ,用户的检索条件都被封装到 searchParam 对象中！
        String urlParam = this.makeUrlParam(searchParam);
        model.addAttribute("urlParam",urlParam);

        //  存储品牌的面包屑： 品牌：品牌的名称 传入的参数：3:华为
        String trademarkParam = this.makeTrademarkParam(searchParam.getTrademark());
        model.addAttribute("trademarkParam",trademarkParam);

        //  平台属性面包屑：
        List<Map> propsParamList = this.makePropsParamList(searchParam.getProps());
        model.addAttribute("propsParamList",propsParamList);

        //  存储排序：
        Map orderMap = this.makeOrderMap(searchParam.getOrder());
        model.addAttribute("orderMap",orderMap);

        //  返回检索视图名
        return "list/index";
    }

    /**
     * 获取排序规则
     * @param order
     * @return
     */
    private Map makeOrderMap(String order) {
        //  声明集合对象
        HashMap<Object, Object> map = new HashMap<>();
        //  判断
        if (!StringUtils.isEmpty(order)){
            //  order=2:asc 对其进行分割
            String[] split = order.split(":");
            if (split!=null && split.length==2){
                //  orderMap.type orderMap.sort
                map.put("type",split[0]);
                map.put("sort",split[1]);
            }
        }else {
            //  当前order 为空，给默认规则
            map.put("type","1");
            map.put("sort","desc");
        }
        //  返回集合对象
        return map;
    }

    /**
     * 制作平台属性面包屑
     * @param props
     * @return
     */
    private List<Map> makePropsParamList(String[] props) {
        //  声明一个集合
        List<Map> list = new ArrayList<>();
        //  判断当前数组是否为空
        if (props!=null && props.length>0){
            //  遍历当前数组
            for (String prop : props) {
                //  prop=3886:256G:内存 对其进行分割
                String[] split = prop.split(":");
                if (split!=null && split.length ==3){
                    //  创建一个map 对象
                    HashMap<String, Object> map = new HashMap<>();
                    map.put("attrId",split[0]);
                    map.put("attrValue",split[1]);
                    map.put("attrName",split[2]);

                    //  将map 添加到list 集合
                    list.add(map);
                }
            }
        }
        //  返回数据
        return list;
    }

    /**
     * 获取品牌面包屑
     * @param trademark
     * @return
     */
    private String makeTrademarkParam(String trademark) {
        //  判断 trademark=2:华为
        if (!StringUtils.isEmpty(trademark)){
            //  对字符串进行分割
            String[] split = trademark.split(":");
            if (split!=null && split.length==2){
                //  直接返回数据
                return "品牌:"+split[1];
            }
        }
        return null;
    }

    /**
     * 记录用户的检索条件
     * @param searchParam
     * @return
     */
    private String makeUrlParam(SearchParam searchParam) {
        //  声明一个StringBuilder 对象。
        StringBuilder sb = new StringBuilder();
        //  记录用户到底使用了哪些检索条件
        //  检索入口有两个 一个是分类Id
        // http://list.gmall.com/list.html?category3Id = 61
        if (!StringUtils.isEmpty(searchParam.getCategory3Id())){
            sb.append("category3Id=").append(searchParam.getCategory3Id());
        }

        if (!StringUtils.isEmpty(searchParam.getCategory2Id())){
            sb.append("category2Id=").append(searchParam.getCategory2Id());
        }

        if (!StringUtils.isEmpty(searchParam.getCategory1Id())){
            sb.append("category1Id=").append(searchParam.getCategory1Id());
        }

        //  一个是全文检索：http://list.gmall.com/list.html?keyword=手机
        if (!StringUtils.isEmpty(searchParam.getKeyword())){
            sb.append("keyword=").append(searchParam.getKeyword());
        }

        //  可以通过品牌检索： http://list.gmall.com/list.html?keyword=手机&trademark=2:华为
        if (!StringUtils.isEmpty(searchParam.getTrademark())){
            //  拼接字符串的长度大于0
            if (sb.length()>0){
                sb.append("&trademark=").append(searchParam.getTrademark());
            }
        }
        //  平台属性：http://list.gmall.com/list.html?keyword=手机&props=3886:256G:内存&props=3588:3000万像素:摄像头像素
        String[] props = searchParam.getProps();
        if (props!=null && props.length>0){
            //  获取的是数组：遍历这个数组中的数据
            for (String prop : props) {
                //  拼接字符串的长度大于0
                if (sb.length()>0){
                    sb.append("&props=").append(prop);
                }
            }
        }
        //  返回数据
        return "list.html?"+sb.toString();
    }
}
