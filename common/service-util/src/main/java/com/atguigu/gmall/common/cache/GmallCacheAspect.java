package com.atguigu.gmall.common.cache;

import com.alibaba.fastjson.JSON;
import com.atguigu.gmall.common.constant.RedisConst;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.Arrays;
import java.util.concurrent.TimeUnit;

/**
 * @author atguigu-mqx
 */
@Component
@Aspect
public class GmallCacheAspect {

    @Autowired
    private RedissonClient redissonClient;

    @Autowired
    private RedisTemplate redisTemplate;

    //  当前这个类应该处理什么工作？  只需要找到那些方法上有我们自定义的注解，
    //  如果找到了，则让这个方法具有分布式锁的业务逻辑 参考 @Transactional！
    @Around("@annotation(com.atguigu.gmall.common.cache.GmallCache)")
    public Object gmallCacheAspcet(ProceedingJoinPoint joinPoint) throws Throwable {
        //  声明一个Object 对象
        Object object = new Object();
        /*
        1.  如何获取到这个注解
        2.  需要组成缓存key！
        3.  根据缓存key 获取对应的缓存数据
            true:
                直接返回
            fasle:
                查询数据库：防止缓存击穿，穿透
         */
        //  先获取到方法的签名
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        //  利用签名获取方法上的注解
        GmallCache gmallCache = signature.getMethod().getAnnotation(GmallCache.class);
        //  组成key 跟注解的前缀有关系！
        String prefix = gmallCache.prefix();
        //  组成key 最好还有方法的参数
        Object[] args = joinPoint.getArgs();
        //  组成缓存的key
        String key = prefix + Arrays.asList(args).toString();
        try {
            //  根据缓存key 获取数据
            object = getCache(key,signature);
            //  判断
            if (object==null){
                //  获取数据库的数据，然后放入缓存！
                //  利用redisson 进行上锁！
                RLock lock = redissonClient.getLock(key + ":lock");
                //  尝试加锁
                boolean res = lock.tryLock(RedisConst.SKULOCK_EXPIRE_PX1,RedisConst.SKULOCK_EXPIRE_PX2, TimeUnit.SECONDS);
                // res = true ：表示获取到了锁
                if (res){
                    try {
                        //  编写业务代码！ 数据库, 那么那个方法?
                        object = joinPoint.proceed(joinPoint.getArgs()); // 这个方法就相当于 带有@GmallCache注解的方法体
                        //  判断object
                        if (object==null){
                            //  数据库中没有对应的数据！
                            Object o = new Object();
                            //  虽然此处使用的是字符串数据类型： 当前value 是 Object ，代码任意数据类型
                            redisTemplate.opsForValue().set(key, JSON.toJSONString(o),RedisConst.SKUKEY_TEMPORARY_TIMEOUT,TimeUnit.SECONDS);
                            return o;
                        }
                        //  object 不是空！
                        redisTemplate.opsForValue().set(key, JSON.toJSONString(object),RedisConst.SKUKEY_TIMEOUT,TimeUnit.SECONDS);
                        //  真正的数据！
                        return object;
                    }finally {
                        lock.unlock();
                    }
                }else {
                    //  睡眠自旋
                    Thread.sleep(100);
                    return gmallCacheAspcet(joinPoint);
                }
            }else {
                return object;
            }
        } catch (Throwable throwable) {
            //  记录日志，发送短信赶紧通知运维人员来修理...
            throwable.printStackTrace();
        }
        //  暂时使用数据库支撑一下！
        return joinPoint.proceed(joinPoint.getArgs());
    }

    /**
     * 通过缓存key 来获取缓存中的数据
     * @param key
     * @return
     */
    private Object getCache(String key,MethodSignature signature) {
        //  demo : BaseCategoryView getBaseCategoryView(Long category3Id)
        //  放入缓存的时候所有数据存储的都是 String
        String strData = (String) redisTemplate.opsForValue().get(key);
        //  判断缓存中是否有数据！
        if (!StringUtils.isEmpty(strData)){
            //  将数据进行返回！
            //  获取到@GmallCache 上的返回值类型！ 获取到之后，将这个strData 转换成当前的数据类型！
            Class returnType = signature.getReturnType(); // BaseCategoryView
            //  strData 转换为 BaseCategoryView
            return JSON.parseObject(strData,returnType);
        }

        return null;
    }


}
