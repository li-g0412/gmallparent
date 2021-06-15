package com.atguigu.gmall.product.controller;

import com.atguigu.gmall.common.result.Result;
import com.atguigu.gmall.model.product.BaseTrademark;
import com.atguigu.gmall.product.service.BaseTrademarkService;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * @author atguigu-mqx
 */
@RestController
@RequestMapping("admin/product/baseTrademark")
public class BaseTrademarkController {

    @Autowired
    private BaseTrademarkService baseTrademarkService;

    //  http://localhost/admin/product/baseTrademark/1/10
    @GetMapping("{page}/{limit}")
    public Result getPage(@PathVariable Long page,
                          @PathVariable Long limit){

        //  创建一个Page 对象
        Page<BaseTrademark> baseTrademarkPage = new Page<BaseTrademark>(page,limit);

        IPage<BaseTrademark> baseTrademarkIPage = baseTrademarkService.getPage(baseTrademarkPage);
        //  返回数据
        return Result.ok(baseTrademarkIPage);
    }

    //  添加品牌数据：
    //  http://api.gmall.com/admin/product/baseTrademark/save
    //  前端传递的是Json 数据 ,需要将其转换为JavaObject
    @PostMapping("save")
    public Result save(@RequestBody BaseTrademark baseTrademark){
        //  保存方法
        baseTrademarkService.save(baseTrademark);
        return Result.ok();
    }

    //  删除数据
    //  http://localhost/admin/product/baseTrademark/remove/11
    @DeleteMapping("remove/{id}")
    public Result remove(@PathVariable Long id){
        //  调用服务层方法
        baseTrademarkService.removeById(id);
        return Result.ok();
    }

    //  回显数据
    //  http://api.gmall.com/admin/product/baseTrademark/get/{id}
    @GetMapping("get/{id}")
    public Result getData(@PathVariable Long id){
        //  调用服务层方法
        BaseTrademark baseTrademark = baseTrademarkService.getById(id);
        return Result.ok(baseTrademark);
    }

    //  修改的按钮
    //  http://api.gmall.com/admin/product/baseTrademark/update
    //  通过api 数据接口文档，得知前端传递的是Json 数据
    @PutMapping("update")
    public Result updateData(@RequestBody BaseTrademark baseTrademark){
        //  调用服务层方法
        baseTrademarkService.updateById(baseTrademark);
        return Result.ok();
    }

    //  http://api.gmall.com/admin/product/baseTrademark/getTrademarkList
    @GetMapping("getTrademarkList")
    public Result getTrademarkList(){
        //  select * from base_trademark
        List<BaseTrademark> list = baseTrademarkService.list(null);
        //  返回
        return Result.ok(list);
    }


}
