package com.atguigu.gmall.product.service;

/**
 * @author atguigu-mqx
 */
public interface TestService {

    //  数据接口
    void testLock();

    //  读锁
    String readLock();
    //  写锁
    String writeLock();
}
