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

/**
 * @author atguigu-mqx
 */
@Component
public class AuthGlobalFilter implements GlobalFilter {


    @Autowired
    private RedisTemplate redisTemplate;

    //  获取到配置文件中的url
    @Value("${authUrls.url}")
    private String authUrls; // authUrls = trade.html,myOrder.html ,list.html
    //  创建一个对象
    private AntPathMatcher antPathMatcher = new AntPathMatcher();

    //  过滤器
    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        //  现在用户不能通过url 直接访问内部数据接口！
        //  http://api.gmall.com/api/product/inner/getSkuInfo/45
        //  获取到请求对象
        ServerHttpRequest request = exchange.getRequest();
        String path = request.getURI().getPath();
        //  判断path
        if (antPathMatcher.match("/**/inner/**",path)){
            //  如何匹配成功，则不能访问这样的路径！
            ServerHttpResponse response = exchange.getResponse();
            //  提示
            return out(response, ResultCodeEnum.PERMISSION);
        }

        //  获取用户Id
        String userId = getUserId(request);
        //  获取临时用户Id
        String userTempId = getUserTempId(request);

        //  附加判断 : 表示登录的服务器Ip 与 缓存中的Id 不一致！
        if("-1".equals(userId)){
            //  获取响应对象
            ServerHttpResponse response = exchange.getResponse();
            //  提示
            return out(response, ResultCodeEnum.PERMISSION);
        }

        //  /api/**/auth/** 如果用户发起了这样的请求，则必须用户是处于登录状态！
        if (antPathMatcher.match("/api/**/auth/**",path)){
            //  判断用户Id 不能为空！
            if (StringUtils.isEmpty(userId)){
                //  如果用户Id 为空，则需要提示，需要登录！
                //  获取响应对象
                ServerHttpResponse response = exchange.getResponse();
                //  提示
                return out(response, ResultCodeEnum.LOGIN_AUTH);
            }
        }
        //  trade.html,myOrder.html,list.html 用户访问这样的控制器，则需要登录，如果没有登录则跳转到登录页面！
        //  包含?  http://list.gmall.com/list.html?category3Id=61
        //  if (path.indexOf(trade.html))
        String[] split = authUrls.split(",");
        //  循环遍历控制器
        for (String url : split) {
            //  判断path 中是否包含对应的控制器  用户访问了对应的控制器，但是，没有登录，则需要跳转到登录页面！
            if (path.indexOf(url)!=-1 && StringUtils.isEmpty(userId)){
                // 设置跳转登录页面！
                ServerHttpResponse response = exchange.getResponse();
                //  设置一些重定向的参数
                response.setStatusCode(HttpStatus.SEE_OTHER);
                //  到登录页面！
                //  https://passport.jd.com/new/login.aspx?ReturnUrl=request.getURI();
                response.getHeaders().set(HttpHeaders.LOCATION,"http://www.gmall.com/login.html?originUrl="+request.getURI());
                //  自定义重定向
                return response.setComplete();
            }
        }

        //  如果用户登录成功了，需要将用户Id 传递到后台微服务！String userId = AuthContextHolder.getUserId(request);
        if (!StringUtils.isEmpty(userId) || !StringUtils.isEmpty(userTempId)){
            //  设置用户Id
            if (!StringUtils.isEmpty(userId)){
                //  需要将userId 传递出去！用户Id 放入请求头中！
                request.mutate().header("userId", userId).build();
            }
            //  设置临时用户Id
            if (!StringUtils.isEmpty(userTempId)){
                //  需要将userId 传递出去！用户Id 放入请求头中！
                request.mutate().header("userTempId", userTempId).build();
            }
            //  exchange  chain  request
            //  将 serverHttpRequest 利用上，  serverHttpRequest 与 ServerWebExchange 转换上！
            //  设置返回   ServerWebExchange
            return chain.filter(exchange.mutate().request(request).build());
        }

        //  返回
        return chain.filter(exchange);
    }

    /**
     * 获取临时用户id
     * @param request
     * @return
     */
    private String getUserTempId(ServerHttpRequest request) {
        String userTempId = "";
        //  临时用户Id 被存在cookie 中！
        HttpCookie httpCookie = request.getCookies().getFirst("userTempId");
        if(httpCookie!=null){
            //  获取数据
            userTempId = httpCookie.getValue();
        }else {
            List<String> stringList = request.getHeaders().get("userTempId");
            if (!CollectionUtils.isEmpty(stringList)){
                userTempId = stringList.get(0);
            }
        }
        //  返回数据！
        return userTempId;
    }

    /**
     * 获取用户Id
     * @param request
     * @return
     */
    private String getUserId(ServerHttpRequest request) {
        //  用户Id 存储在缓存中的！ 通过key 来获取userId!
        //  String userLoginKey = RedisConst.USER_LOGIN_KEY_PREFIX + token; 关键在token 上！
        //  登录的时候，token 可能在cookie 中，也有可能在header 中！
        String token = "";
        //  List<HttpCookie> cookieList = request.getCookies().get("token");
        HttpCookie httpCookie = request.getCookies().getFirst("token");
        //  说明cookie 中有数据
        if (httpCookie!=null){
            token = httpCookie.getValue();
        }else {
            //  说明cookie 中没有数据，从header 中获取！
            List<String> stringList = request.getHeaders().get("token");
            if (!StringUtils.isEmpty(stringList)){
                token = stringList.get(0);
            }
        }
        //  获取到token了
        if (!StringUtils.isEmpty(token)){
            //  组成缓存组成的key！ String userLoginKey = RedisConst.USER_LOGIN_KEY_PREFIX + token;
            String userLoginKey = "user:login:" + token;
            //  从缓存中获取数据
            String strJson = (String) redisTemplate.opsForValue().get(userLoginKey);
            //  这个字符串的本质：JSONObject
            JSONObject jsonObject = JSON.parseObject(strJson, JSONObject.class);
            //  先获取ip 地址
            String ip = (String) jsonObject.get("ip");
            if (ip.equals(IpUtil.getGatwayIpAddress(request))){
                //  获取到userId
                String userId = (String) jsonObject.get("userId");
                //  返回用户Id
                return userId;
            }else {
                return "-1";
            }
        }
        return null;
    }

    /**
     * 拒绝方法!
     * @param response
     * @param resultCodeEnum
     * @return
     */
    private Mono<Void> out(ServerHttpResponse response, ResultCodeEnum resultCodeEnum) {
        //  信息提示方法！ resultCodeEnum这个对象中的内容！
        Result<Object> result = Result.build(null, resultCodeEnum);
        //  将这个对象中的内容进行输出 result 转换为String 类型
        String string = JSON.toJSONString(result);
        DataBuffer wrap = response.bufferFactory().wrap(string.getBytes());
        //  要想输入数据，我们最好设置一些输出的格式！
        response.getHeaders().add("Content-Type","application/json;charset=UTF-8");
        //  处理：wrap --> Publisher
        //  将提示信息输入到页面上！ Publisher
        return response.writeWith(Mono.just(wrap));
    }
}
