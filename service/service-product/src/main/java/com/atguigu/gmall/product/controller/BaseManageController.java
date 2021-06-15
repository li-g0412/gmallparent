package com.atguigu.gmall.product.controller;

import com.atguigu.gmall.common.result.Result;
import com.atguigu.gmall.model.product.BaseAttrInfo;
import com.atguigu.gmall.model.product.BaseCategory1;
import com.atguigu.gmall.product.service.ManageService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * @author atguigu-mqx
 */
@RestController
@RequestMapping("admin/product")
public class BaseManageController {

    //  注入服务层
    @Autowired
    private ManageService manageService;

    //  http://localhost/admin/product/getCategory1
    //  获取一级分类数据：
    @GetMapping("getCategory1")
    public Result getCategory1(){
        //  获取服务层的数据
        List<BaseCategory1> baseCategory1List = manageService.getBaseCategory1();
        //  根据前端数据接口，返回对应的result.data;
        return Result.ok(baseCategory1List);
    }

    //   获取二级分类数据
    //  http://api.gmall.com/admin/product/getCategory2/{category1Id}
    @GetMapping("getCategory2/{category1Id}")
    public Result getCategory2(@PathVariable Long category1Id){
        //  获取服务层的数据
        return Result.ok(manageService.getBaseCategory2(category1Id));
    }

    //  获取三级分类数据
    //  http://api.gmall.com/admin/product/getCategory3/{category2Id}
    @GetMapping("getCategory3/{category2Id}")
    public Result getCategory3(@PathVariable Long category2Id){
        //  获取服务层的数据
        return Result.ok(manageService.getBaseCategory3(category2Id));
    }

    //  根据分类Id 获取平台属性集合
    //  http://api.gmall.com/admin/product/attrInfoList/{category1Id}/{category2Id}/{category3Id}
    @GetMapping("attrInfoList/{category1Id}/{category2Id}/{category3Id}")
    public Result getAttrInfoList(@PathVariable Long category1Id,
                                  @PathVariable Long category2Id,
                                  @PathVariable Long category3Id){
        //  调用服务层方法
        List<BaseAttrInfo> baseAttrInfoList = manageService.getBaseAttrInfoList(category1Id, category2Id, category3Id);

        //  返回数据
        return Result.ok(baseAttrInfoList);

    }

    //  保存平台属性控制器 http://localhost/admin/product/saveAttrInfo
    //  http://api.gmall.com/admin/product/saveAttrInfo
    //  前端传递的参数转换成哪个实体类?
    //  1.  可以自定义class 实体类！ 2.  如果实体类中有类似的Json 数据结构，那么我们就可以直接使用！
    @PostMapping("saveAttrInfo")
    public Result saveAttrInfo(@RequestBody BaseAttrInfo baseAttrInfo){
        //  调用服务层方法
        manageService.saveAttrInfo(baseAttrInfo);

        //  返回
        return Result.ok();

    }

    //  根据平台属性Id 获取到平台属性值集合
    //  http://api.gmall.com/admin/product/getAttrValueList/{attrId}
    @GetMapping("getAttrValueList/{attrId}")
    public Result getAttrValueList(@PathVariable Long attrId){
        //  调用服务层方法
        //  从业务角度出发：回显的是平台属性值集合， 平台属性：属性值  1 ：n ，也就说只有1，才能有多！
        //  必须有平台属性，那么才能可以查询到平台属性值！
        //  select * from base_attr_info where id = attrId;   baseAttrInfo;
        //  if（baseAttrInfo != null） 获取到平台属性值集合   return baseAttrInfo.getAttrValueList();
        BaseAttrInfo baseAttrInfo = manageService.getBaseAttrInfo(attrId);

        //        List<BaseAttrValue> baseAttrValueList = manageService.getAttrValueList(attrId);
        //        //  返回数据
        //        return Result.ok(baseAttrValueList);

        return  Result.ok(baseAttrInfo.getAttrValueList());
    }


}
