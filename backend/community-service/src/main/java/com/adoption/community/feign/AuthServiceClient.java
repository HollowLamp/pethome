package com.adoption.community.feign;

import com.adoption.common.api.ApiResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.Map;

/**
 * Feign 客户端：调用 auth-service 获取用户信息
 *
 * 用途：
 * - 根据角色获取用户列表
 * - 获取用户基本信息
 */
@FeignClient(name = "auth-service", path = "/auth")
public interface AuthServiceClient {

    /**
     * 获取用户列表（分页）
     *
     * 注意：这个接口需要 ADMIN 权限，但在服务间调用时可以通过网关配置允许内部调用
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

    /**
     * 根据用户ID获取用户信息
     *
     * @param userId 用户ID
     * @return 用户信息，包含username等字段
     */
    @GetMapping("/users/{id}")
    ApiResponse<Map<String, Object>> getUserById(@PathVariable("id") Long userId);
}

