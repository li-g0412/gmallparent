package com.atguigu.gmall.user.controller;

import com.alibaba.fastjson.JSONObject;
import com.atguigu.gmall.common.constant.RedisConst;
import com.atguigu.gmall.common.result.Result;
import com.atguigu.gmall.common.util.IpUtil;
import com.atguigu.gmall.model.user.UserInfo;
import com.atguigu.gmall.user.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;
import java.util.HashMap;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

/**
 * @author atguigu-mqx
 */
@RestController
@RequestMapping("/api/user/passport")
public class PassportApiController {

    @Autowired
    private UserService userService;

    @Autowired
    private RedisTemplate redisTemplate;


    //  有个登录方法
    @PostMapping("login")
    public Result login(@RequestBody UserInfo userInfo, HttpServletRequest request){

        //  调用服务层方法
        UserInfo info = userService.login(userInfo);
        //  判断
        if (info!=null){
            //  一定有一个token 值！ ，同时还需要存储一个 data ！ 那么这个data 是map 集合！
            String token = UUID.randomUUID().toString();
            //  声明一个map 集合
            HashMap<String, Object> map = new HashMap<>();
            map.put("token",token);
            map.put("nickName",info.getNickName());
            //  将用户相关信息存储到缓存中！
            //  哪种数据类型 String ，以及key ！ 在缓存中只需要存储一个关键信息！ Long userId = info.getId();
            //  key 很关键！ 跟token 有关系！ user:login:token
            //  token 被存储到哪? cookie ,cookie 不安全！ 因此：做一个ip 地址！与 userId 一起存到缓存中！
            JSONObject jsonObject = new JSONObject();
            jsonObject.put("ip", IpUtil.getIpAddress(request));
            jsonObject.put("userId",info.getId().toString());

            //  需要将jsonObject 这个对象存储到缓存！
            String userLoginKey = RedisConst.USER_LOGIN_KEY_PREFIX + token;
            redisTemplate.opsForValue().set(userLoginKey,jsonObject.toJSONString(),RedisConst.USERKEY_TIMEOUT, TimeUnit.SECONDS);

            //  返回result!
            return Result.ok(map);
        }else {
            //  没有登录成功提示信息！
            return Result.fail().message("用户名或密码不正确!");
        }


    }
}
