package com.adoption.ai.feign;

import com.adoption.common.api.ApiResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;

import java.util.Map;

/**
 * Feign 客户端：调用 pet-service
 *
 * 用于更新宠物健康状态
 */
@FeignClient(name = "pet-service", path = "/pets")
public interface PetServiceClient {
    /**
     * C端用户上传健康状态（需要验证用户是否领养了该宠物）
     * POST /pets/{id}/health/owner
     */
    @PostMapping("/{id}/health/owner")
    ApiResponse<Map<String, Object>> updateHealthByOwner(
            @RequestHeader("X-User-Id") Long userId,
            @PathVariable("id") Long petId,
            @RequestBody Map<String, Object> health);
}

