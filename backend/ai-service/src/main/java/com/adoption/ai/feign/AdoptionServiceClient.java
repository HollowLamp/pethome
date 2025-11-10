package com.adoption.ai.feign;

import com.adoption.common.api.ApiResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

/**
 * Feign 客户端：调用 adoption-service
 *
 * 用于验证用户是否领养了宠物
 */
@FeignClient(name = "adoption-service", path = "/adoptions")
public interface AdoptionServiceClient {
    /**
     * 检查用户是否领养了该宠物
     */
    @GetMapping("/check-ownership")
    ApiResponse<Boolean> checkOwnership(
            @RequestParam("petId") Long petId,
            @RequestParam("userId") Long userId);
}

