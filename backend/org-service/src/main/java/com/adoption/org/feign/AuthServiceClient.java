package com.adoption.org.feign;

import com.adoption.common.api.ApiResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.Map;

/**
 * Feign 客户端：调用 auth-service 获取用户信息
 */
@FeignClient(name = "auth-service", path = "/auth")
public interface AuthServiceClient {

    /**
     * 根据用户ID获取用户信息
     * @param userId 用户ID
     * @return 用户信息
     */
    @GetMapping("/users/{id}")
    ApiResponse<Map<String, Object>> getUserById(@PathVariable("id") Long userId);
}

