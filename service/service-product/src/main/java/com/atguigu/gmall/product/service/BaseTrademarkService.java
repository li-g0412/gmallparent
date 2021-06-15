package com.atguigu.gmall.product.service;

import com.atguigu.gmall.model.product.BaseTrademark;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;

/**
 * @author atguigu-mqx
 */
public interface BaseTrademarkService extends IService<BaseTrademark> {
    /**
     *  数据接口：带分页的查询！
     * @param page
     * @return
     */
    IPage<BaseTrademark> getPage(Page<BaseTrademark> page);

}
