package com.adoption.gateway.filter;

import com.adoption.common.util.JwtUtils;
import io.jsonwebtoken.Claims;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.util.List;

@Component
public class JwtAuthFilter implements GlobalFilter, Ordered {

    @Value("${jwt.secret}")
    private String secret;

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        String path = exchange.getRequest().getURI().getPath();

        // ==== 无需登陆的请求 ====
        if (path.startsWith("/auth/login") || path.startsWith("/auth/register")) {
            System.out.println("[网关] 放行无需登录的请求: " + path);
            return chain.filter(exchange);
        }

        String authHeader = exchange.getRequest().getHeaders().getFirst(HttpHeaders.AUTHORIZATION);
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            System.out.println("[网关] 未携带或格式错误的认证头，拒绝访问: " + path);
            exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
            return exchange.getResponse().setComplete();
        }

        String token = authHeader.substring(7);
        try {
            Claims claims = JwtUtils.parseToken(token, secret);
            String userId = claims.getSubject();
            List<String> roles = claims.get("roles", List.class);

            System.out.println("[网关] 令牌解析成功，用户ID=" + userId + ", 角色=" + roles + ", 请求路径=" + path);

            // ==== RBAC auth模块 ====
            // 只有超级管理员能分配角色
            if (path.startsWith("/auth/roles") && !roles.contains("ADMIN")) {
                System.out.println("[网关] 权限不足：尝试访问角色分配接口，已拒绝，用户ID=" + userId);
                exchange.getResponse().setStatusCode(HttpStatus.FORBIDDEN);
                return exchange.getResponse().setComplete();
            }

            // 只有超级管理员能查询用户角色
            if (path.startsWith("/auth/users/") && path.endsWith("/roles") && !roles.contains("ADMIN")) {
                System.out.println("[网关] 权限不足：尝试查询用户角色，已拒绝，用户ID=" + userId);
                exchange.getResponse().setStatusCode(HttpStatus.FORBIDDEN);
                return exchange.getResponse().setComplete();
            }

            // ==== RBAC xx模块 ====


            // 把用户信息透传下去（如果子服务需要知道是谁）
            exchange = exchange.mutate().request(
                    r -> r.headers(h -> {
                        h.add("X-User-Id", userId);
                        h.add("X-Roles", String.join(",", roles));
                    })
            ).build();

            System.out.println("[网关] 权限校验通过，转发请求: " + path);
            return chain.filter(exchange);

        } catch (Exception e) {
            System.out.println("[网关] 令牌解析或权限校验异常，已拒绝: " + e.getMessage());
            exchange.getResponse().setStatusCode(HttpStatus.FORBIDDEN);
            return exchange.getResponse().setComplete();
        }
    }

    @Override
    public int getOrder() {
        return -1;
    }
}
