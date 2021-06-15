package com.atguigu.gmall.common.cache;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * @author atguigu-mqx
 */
@Target(ElementType.METHOD) //  注解在什么位置可以使用！
@Retention(RetentionPolicy.RUNTIME) // 注解的声明周期
public @interface GmallCache {

    //  表示前缀: 目的组成缓存的key！
    String prefix() default "cache";
}
