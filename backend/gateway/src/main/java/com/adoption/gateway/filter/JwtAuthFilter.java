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
        // 宠物列表和详情无需登录，但需要排除需要登录的路径
        boolean isPublicPetPath = path.startsWith("/pets") &&
            !path.contains("/wishlist") &&
            !path.contains("/health") &&
            !path.contains("/feedbacks") &&
            !path.startsWith("/pets/org");

        if (path.startsWith("/auth/login") ||
            path.startsWith("/auth/register") ||
            path.startsWith("/files/") || // 文件访问无需登录
            isPublicPetPath) { // 宠物列表和详情无需登录
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

            // 获取请求方法（用于区分同一路径的不同HTTP方法）
            String requestMethod = exchange.getRequest().getMethod().name();

            System.out.println("[网关] 令牌解析成功，用户ID=" + userId + ", 角色=" + roles + ", 请求路径=" + path + ", 方法=" + requestMethod);

            // ==== RBAC auth模块 ====
            // 只有超级管理员能分配角色
            if (path.startsWith("/auth/roles") && !roles.contains("ADMIN")) {
                System.out.println("[网关] 权限不足：尝试访问角色分配接口，已拒绝，用户ID=" + userId);
                exchange.getResponse().setStatusCode(HttpStatus.FORBIDDEN);
                return exchange.getResponse().setComplete();
            }

            // ==== RBAC org模块 ====
            // POST /org/apply - 只有 ORG_ADMIN 可以申请机构入驻
            if (path.equals("/org/apply") && requestMethod.equals("POST")
                    && !roles.contains("ORG_ADMIN")) {
                System.out.println("[网关-org] 权限不足：申请机构入驻需要 ORG_ADMIN 角色，用户ID=" + userId);
                exchange.getResponse().setStatusCode(HttpStatus.FORBIDDEN);
                return exchange.getResponse().setComplete();
            }

            // POST /org/{id}/approve - 只有 CS 可以审核通过机构申请
            if (path.matches("^/org/\\d+/approve$") && requestMethod.equals("POST")
                    && !roles.contains("CS")) {
                System.out.println("[网关-org] 权限不足：审核通过机构申请需要 CS 角色，用户ID=" + userId);
                exchange.getResponse().setStatusCode(HttpStatus.FORBIDDEN);
                return exchange.getResponse().setComplete();
            }

            // POST /org/{id}/reject - 只有 CS 可以拒绝机构申请
            if (path.matches("^/org/\\d+/reject$") && requestMethod.equals("POST")
                    && !roles.contains("CS")) {
                System.out.println("[网关-org] 权限不足：拒绝机构申请需要 CS 角色，用户ID=" + userId);
                exchange.getResponse().setStatusCode(HttpStatus.FORBIDDEN);
                return exchange.getResponse().setComplete();
            }

            // GET /org/{id} - 只有 ORG_ADMIN 可以查询机构详情
            if (path.matches("^/org/\\d+$") && requestMethod.equals("GET")
                    && !roles.contains("ORG_ADMIN")) {
                System.out.println("[网关-org] 权限不足：查询机构详情需要 ORG_ADMIN 角色，用户ID=" + userId);
                exchange.getResponse().setStatusCode(HttpStatus.FORBIDDEN);
                return exchange.getResponse().setComplete();
            }

            // POST /org/{id}/members - 只有 ORG_ADMIN 可以添加机构成员
            if (path.matches("^/org/\\d+/members$") && requestMethod.equals("POST")
                    && !roles.contains("ORG_ADMIN")) {
                System.out.println("[网关-org] 权限不足：添加机构成员需要 ORG_ADMIN 角色，用户ID=" + userId);
                exchange.getResponse().setStatusCode(HttpStatus.FORBIDDEN);
                return exchange.getResponse().setComplete();
            }

            // DELETE /org/{id}/members/{uid} - 只有 ORG_ADMIN 可以删除机构成员
            if (path.matches("^/org/\\d+/members/\\d+$") && requestMethod.equals("DELETE")
                    && !roles.contains("ORG_ADMIN")) {
                System.out.println("[网关-org] 权限不足：删除机构成员需要 ORG_ADMIN 角色，用户ID=" + userId);
                exchange.getResponse().setStatusCode(HttpStatus.FORBIDDEN);
                return exchange.getResponse().setComplete();
            }

            // GET /org/{id}/members - 只有 ORG_ADMIN 可以查询机构成员列表
            if (path.matches("^/org/\\d+/members$") && requestMethod.equals("GET")
                    && !roles.contains("ORG_ADMIN")) {
                System.out.println("[网关-org] 权限不足：查询机构成员列表需要 ORG_ADMIN 角色，用户ID=" + userId);
                exchange.getResponse().setStatusCode(HttpStatus.FORBIDDEN);
                return exchange.getResponse().setComplete();
            }


            // ==== RBAC pet模块 ====
            // POST /pets/org - 只有机构管理员可以创建宠物
            if (path.equals("/pets/org") && "POST".equals(exchange.getRequest().getMethod().name()) && !roles.contains("ORG_ADMIN")) {
                System.out.println("[网关] 权限不足：尝试创建宠物，已拒绝，用户ID=" + userId);
                exchange.getResponse().setStatusCode(HttpStatus.FORBIDDEN);
                return exchange.getResponse().setComplete();
            }

            // PATCH /pets/org/{id} - 机构管理员或维护员可以修改宠物信息
            if (path.matches("/pets/org/\\d+") && "PATCH".equals(exchange.getRequest().getMethod().name())
                && !roles.contains("ORG_ADMIN") && !roles.contains("ORG_STAFF")) {
                System.out.println("[网关] 权限不足：尝试修改宠物信息，已拒绝，用户ID=" + userId);
                exchange.getResponse().setStatusCode(HttpStatus.FORBIDDEN);
                return exchange.getResponse().setComplete();
            }

            // POST /pets/org/{id}/status - 只有机构管理员可以修改宠物状态
            if (path.matches("/pets/org/\\d+/status") && "POST".equals(exchange.getRequest().getMethod().name()) && !roles.contains("ORG_ADMIN")) {
                System.out.println("[网关] 权限不足：尝试修改宠物状态，已拒绝，用户ID=" + userId);
                exchange.getResponse().setStatusCode(HttpStatus.FORBIDDEN);
                return exchange.getResponse().setComplete();
            }

            // POST /pets/org/{id}/cover - 机构管理员或维护员可以上传封面图
            if (path.matches("/pets/org/\\d+/cover") && "POST".equals(exchange.getRequest().getMethod().name())
                && !roles.contains("ORG_ADMIN") && !roles.contains("ORG_STAFF")) {
                System.out.println("[网关] 权限不足：尝试上传封面图，已拒绝，用户ID=" + userId);
                exchange.getResponse().setStatusCode(HttpStatus.FORBIDDEN);
                return exchange.getResponse().setComplete();
            }

            // POST /pets/{id}/health - 只有维护员可以更新健康记录
            if (path.matches("/pets/\\d+/health") && "POST".equals(exchange.getRequest().getMethod().name()) && !roles.contains("ORG_STAFF")) {
                System.out.println("[网关] 权限不足：尝试更新健康记录，已拒绝，用户ID=" + userId);
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
