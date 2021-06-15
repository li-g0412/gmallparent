package com.atguigu.gmall.gateway.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.reactive.CorsWebFilter;
import org.springframework.web.cors.reactive.UrlBasedCorsConfigurationSource;

/**
 * @author atguigu-mqx
 */
@Configuration
public class CorsConfig {

    //  需要将这个实体类注入到spring ioc 容器
    @Bean
    public CorsWebFilter corsWebFilter(){
        CorsConfiguration corsConfiguration = new CorsConfiguration();
        corsConfiguration.addAllowedOrigin("*");    //  允许跨域的域
        corsConfiguration.addAllowedMethod("*");    //  允许的跨域方法
        corsConfiguration.addAllowedHeader("*");    //  允许携带请求头信息
        corsConfiguration.setAllowCredentials(true);    //  允许携带cookie

        //  创建对象 CorsConfigurationSource
        UrlBasedCorsConfigurationSource urlBasedCorsConfigurationSource = new UrlBasedCorsConfigurationSource();
        //  cors 跨域的本质：设置信息，同时设置path
        urlBasedCorsConfigurationSource.registerCorsConfiguration("/**",corsConfiguration);
        return new CorsWebFilter(urlBasedCorsConfigurationSource);
    }
}
