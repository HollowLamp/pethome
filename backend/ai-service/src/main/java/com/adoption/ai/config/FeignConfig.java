package com.adoption.ai.config;

import feign.RequestInterceptor;
import feign.RequestTemplate;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Feign 配置类
 *
 * 用于配置 Feign 客户端的请求拦截器
 */
@Configuration
public class FeignConfig {

    /**
     * Feign 请求拦截器
     *
     * 注意：在 AI 服务中，我们通过方法参数显式传递 X-User-Id，
     * 所以这里不需要从请求上下文中获取
     */
    @Bean
    public RequestInterceptor requestInterceptor() {
        return new RequestInterceptor() {
            @Override
            public void apply(RequestTemplate template) {
                // AI 服务通过方法参数显式传递请求头，这里不需要额外处理
                // 如果需要传递其他通用请求头，可以在这里添加
            }
        };
    }
}

