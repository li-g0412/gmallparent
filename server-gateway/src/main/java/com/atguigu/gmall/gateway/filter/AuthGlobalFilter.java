package com.atguigu.gmall.gateway.filter;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.atguigu.gmall.common.result.Result;
import com.atguigu.gmall.common.result.ResultCodeEnum;
import com.atguigu.gmall.common.util.IpUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.HttpCookie;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.util.AntPathMatcher;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.util.List;

@Component
public class AuthGlobalFilter implements GlobalFilter {
    @Autowired
    private RedisTemplate redisTemplate;

    @Value("{authUrls.url}")
    private String authUrls;

    private AntPathMatcher antPathMatcher = new AntPathMatcher();

    // 过滤器
    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        // 限制用户不能通过url直接访问内部接口
        ServerHttpRequest request = exchange.getRequest();
        String path = request.getURI().getPath();
        // 判断path
        if (antPathMatcher.match("/**/inner/**", path)) {
            // 匹配成功不能访问
            ServerHttpResponse response = exchange.getResponse();

            return out(response, ResultCodeEnum.PERMISSION);
        }

        // 获取用户id
        String userId = getUserId(request);
        String userTempId = getUserTempId(request);

        // 附加判断
        if ("-1".equals(userId)) {
            // 获取响应对象
            ServerHttpResponse response = exchange.getResponse();
            return out(response, ResultCodeEnum.PERMISSION);
        }

        //   /api/**/auth/**
        if (antPathMatcher.match("/api/**/auth/**", path)) {
            // 判断用户id不能为空
            if (StringUtils.isEmpty(userId)) {
                // 如果为空需要提示登录
                ServerHttpResponse response = exchange.getResponse();
                return out(response, ResultCodeEnum.LOGIN_AUTH);
            }
        }

        // 访问对应页面需要登录
        // 包含
        String[] split = authUrls.split(",");
        for (String url : split) {
            if (path.indexOf(url) != -1 && StringUtils.isEmpty(userId)) {
                // 设置跳转到登录页面
                ServerHttpResponse response = exchange.getResponse();
                // 设置一些重定向的参数
                response.setStatusCode(HttpStatus.SEE_OTHER);
                // 到登录页面
                request.getURI();
                response
                        .getHeaders()
                        .set(
                                HttpHeaders.LOCATION,
                                "http://www.gmall.com/login.html?originUrl=" + request.getURI());

                // 自定义重定向
                return response.setComplete();
            }
        }

        // 登陆成功将用户id传递到后台的微服务
        if (!StringUtils.isEmpty(userId) || !StringUtils.isEmpty(userTempId)) {
            //设置用户id
            if (!StringUtils.isEmpty(userId)) {
                // 需要将userId传递出去，用户id放到请求头
                request.mutate().header("userId", userId).build();
            }
            //设置临时用户id
            if (!StringUtils.isEmpty(userTempId)) {
                // 需要将userId传递出去，用户id放到请求头
                request.mutate().header("userTempId", userTempId).build();
            }

            // 设置返回
            return chain.filter(exchange.mutate().request(request).build());
        }

        return chain.filter(exchange);
    }

    /**
     * 获取当前用户临时用户id
     *
     * @param request
     * @return
     */
    private String getUserTempId(ServerHttpRequest request) {
        HttpCookie httpCookie = request.getCookies().getFirst("userTempId");
        String userTempId = "";
        if (httpCookie != null) {
            //获取数据
            userTempId = httpCookie.getValue();
        } else {
            List<String> stringList = request.getHeaders().get("userTempId");
            if (!CollectionUtils.isEmpty(stringList)) {
                userTempId = stringList.get(0);
            }
        }
        return userTempId;
    }

    /**
     * 获取当前用户id
     *
     * @param request
     * @return
     */
    private String getUserId(ServerHttpRequest request) {
        // 用户id存储在缓存中，通过key获取
        String token = "";
        //        List<HttpCookie> cookieList = request.getCookies().get("token");
        HttpCookie httpCookie = request.getCookies().getFirst("token");
        if (httpCookie != null) {
            token = httpCookie.getValue();
        } else {
            // 说明cookie中没有数据从header中获取
            List<String> stringList = request.getHeaders().get("token");
            if (!StringUtils.isEmpty(stringList)) {
                token = stringList.get(0);
            }
        }
        if (!StringUtils.isEmpty(token)) {
            String userLoginKey = "user:login:" + token;
            String strJson = (String) redisTemplate.opsForValue().get(userLoginKey);

            JSONObject jsonObject = JSON.parseObject(strJson, JSONObject.class);
            String ip = jsonObject.getString("ip");
            String curIp = IpUtil.getGatwayIpAddress(request);
            // 校验token是否被盗用
            if (ip.equals(curIp)) {
                return jsonObject.getString("userId");
            } else {
                // ip不一致
                return "-1";
            }
        }

        return null;
    }

    /**
     * 拒绝方法
     *
     * @param response
     * @param resultCodeEnum
     * @return
     */
    private Mono<Void> out(ServerHttpResponse response, ResultCodeEnum resultCodeEnum) {
        Result<Object> result = Result.build(null, resultCodeEnum);
        String string = JSON.toJSONString(result);
        DataBuffer wrap = response.bufferFactory().wrap(string.getBytes());
        response.getHeaders().add("Content-Type", "application/json;charset=UTF-8");
        // 输入到页面
        return response.writeWith(Mono.just(wrap));
    }
}
