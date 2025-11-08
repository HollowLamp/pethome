package com.adoption.org.feign;

import com.adoption.common.api.ApiResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

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

    /**
     * 获取用户列表（分页）
     * 用于根据角色筛选用户，发送通知
     *
     * @param page 页码，从1开始
     * @param pageSize 每页大小
     * @return 用户列表，包含用户信息和角色信息
     */
    @GetMapping("/users")
    ApiResponse<Map<String, Object>> getUserList(
            @RequestParam("page") int page,
            @RequestParam("pageSize") int pageSize
    );
}

