package com.adoption.gateway.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.web.server.SecurityWebFilterChain;

@Configuration
public class SecurityConfig {

    @Bean
    public SecurityWebFilterChain springSecurityFilterChain(ServerHttpSecurity http) {
        http
                .csrf(ServerHttpSecurity.CsrfSpec::disable)  // 禁用 CSRF
                .authorizeExchange(exchange -> exchange
                        .pathMatchers("/auth/login", "/auth/register").permitAll() // 登录注册放行
                        .anyExchange().permitAll() // 其他交给 JwtAuthFilter
                );

        return http.build();
    }
}
