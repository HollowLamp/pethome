package com.adoption.adoption.feign;

import com.adoption.common.api.ApiResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;

import java.util.Map;

/**
 * Feign 客户端：调用 pet-service 更新宠物状态
 */
@FeignClient(name = "pet-service", path = "/pets/org")
public interface PetServiceClient {

    /**
     * 更新宠物状态
     * @param petId 宠物ID
     * @param body 请求体，包含 status 字段
     * @return 更新结果
     */
    @PostMapping("/{id}/status")
    ApiResponse<String> updatePetStatus(@PathVariable("id") Long petId, @RequestBody Map<String, String> body);
}

