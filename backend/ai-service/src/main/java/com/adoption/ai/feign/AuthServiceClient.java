package com.adoption.ai.feign;

import com.adoption.common.api.ApiResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.Map;

/**
 * Feign 客户端：调用 auth-service
 *
 * 用于获取用户信息（用于通知功能）
 */
@FeignClient(name = "auth-service", path = "/auth")
public interface AuthServiceClient {
    /**
     * 获取用户列表（分页）
     */
    @GetMapping("/users")
    ApiResponse<Map<String, Object>> getUserList(
            @RequestParam("page") int page,
            @RequestParam("pageSize") int pageSize
    );
}

