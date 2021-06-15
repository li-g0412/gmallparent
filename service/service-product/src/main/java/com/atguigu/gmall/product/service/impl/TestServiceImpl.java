package com.atguigu.gmall.product.service.impl;

import com.atguigu.gmall.product.service.TestService;
import org.redisson.api.RLock;
import org.redisson.api.RReadWriteLock;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.UUID;
import java.util.concurrent.TimeUnit;

/**
 * @author atguigu-mqx
 */
@Service
public class TestServiceImpl implements TestService {

    //  谁能操作缓存！
    @Autowired
    private StringRedisTemplate redisTemplate;

    @Autowired
    private RedissonClient redissonClient;

    @Override
    public void testLock() {
        //  第一步：获取到锁
        RLock lock = redissonClient.getLock("mylock");
        try {
            //  上锁：
            lock.lock();
            //  lock.lock(10, TimeUnit.SECONDS);
            //  业务逻辑：
            //  get num
            String num = redisTemplate.opsForValue().get("num");

            //  判断
            if (StringUtils.isEmpty(num)){

                //  直接返回
                return;
            }
            //  对num 进行数据类型转换
            int numValue = Integer.parseInt(num);

            //  对num 进行+1 ，放入缓存 set key value;
            redisTemplate.opsForValue().set("num",String.valueOf(numValue+1));
        } catch (NumberFormatException e) {
            e.printStackTrace();
        } finally {
            //  解锁：
            lock.unlock();
        }

    }

    @Override
    public String readLock() {
        //  获取到锁对象
        RReadWriteLock rwlock = redissonClient.getReadWriteLock("anyRWLock");
        //  上锁！
        rwlock.readLock().lock(10, TimeUnit.SECONDS);
        //  获取到读取的数据
        String msg = redisTemplate.opsForValue().get("msg");

        //  返回读取的内容
        return msg;
    }

    @Override
    public String writeLock() {
        //  获取到锁对象
        RReadWriteLock rwlock = redissonClient.getReadWriteLock("anyRWLock");
        //  上锁！
        rwlock.writeLock().lock(10, TimeUnit.SECONDS);
        //  向缓存中写入数据
        redisTemplate.opsForValue().set("msg", UUID.randomUUID().toString());
        //  返回数据
        return "写入完成!";
    }


    //    @Override
        //    public void testLock() {
        //        /*
        //        1.  先从缓存中获取key = num 所对应的数据！
        //        2.  如果获取到了数据不为空，则对其进行+1 操作！
        //        3.  如果获取到的数据是空，则直接return！
        //         */
        //        //  使用setnx:
        //        //  Boolean flag = redisTemplate.opsForValue().setIfAbsent("lock", "OK");
        //        String uuid = UUID.randomUUID().toString();
        //        Boolean flag = redisTemplate.opsForValue().setIfAbsent("lock",uuid,1, TimeUnit.SECONDS);
        //        //  flag = true : 说明获取到锁了！
        //        if (flag){
        //            //  get num
        //            String num = redisTemplate.opsForValue().get("num");
        //
        //            //  判断
        //            if (StringUtils.isEmpty(num)){
        //
        //                //  直接返回
        //                return;
        //            }
        //            //  对num 进行数据类型转换
        //            int numValue = Integer.parseInt(num);
        //
        //            //  对num 进行+1 ，放入缓存 set key value;
        //            redisTemplate.opsForValue().set("num",String.valueOf(numValue+1));
        //
        //            //  设置过期时间 这种方式缺乏原子性！
        //            //  redisTemplate.expire("lock",100, TimeUnit.SECONDS);
        //            //            if (uuid.equals(redisTemplate.opsForValue().get("lock"))){
        //            //                //  删除锁 index1，index1删除之前，lock 锁过期，index2 获取到资源，redis 的uuid 就是index2的！
        //            //                redisTemplate.delete("lock");
        //            //            }
        //
        //            //  第一个参数； RedisScript
        //            DefaultRedisScript<Long> redisScript = new DefaultRedisScript<>();
        //            //  使用lua 脚本：
        //            String script = "if redis.call('get', KEYS[1]) == ARGV[1] then return redis.call('del', KEYS[1]) else return 0 end";
        //            //  放入lua 脚本！
        //            redisScript.setScriptText(script);
        //            //  设置返回的类型
        //            redisScript.setResultType(Long.class);
        //            // 第一个参数； RedisScript 第二个参数应该是key ，第三个参数应该是value！
        //            redisTemplate.execute(redisScript, Arrays.asList("lock"),uuid);
        //
        //        } else {
        //            //  没有获取到锁的人等待，
        //            try {
        //                Thread.sleep(100);
        //                //  自旋
        //                testLock();
        //            } catch (InterruptedException e) {
        //                e.printStackTrace();
        //            }
        //        }
        //    }
}
