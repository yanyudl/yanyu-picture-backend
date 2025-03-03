package com.ityanyu.yanyupicturebackend.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * @author: dl
 * @Date: 2025/3/1
 * @Description: 跨域配置类
 **/
@Configuration
public class CorsConfig implements WebMvcConfigurer {

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        // TODO 覆盖所有请求
        registry.addMapping("/**")
                //TODO 允许发送cookie
                .allowCredentials(true)
                //放行哪些域名（必须用 Patterns，否则 * 会和 allowCredentials冲突）
                .allowedOriginPatterns("*")
                .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS")
                .allowedHeaders("*")
                .exposedHeaders("*");
    }
}
