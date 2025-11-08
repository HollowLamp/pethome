package com.adoption.pet.feign;

import com.adoption.common.api.ApiResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;
import java.util.Map;

/**
 * Feign 客户端：调用 adoption-service 验证用户是否领养了宠物
 */
@FeignClient(name = "adoption-service", path = "/adoptions")
public interface AdoptionServiceClient {

    /**
     * 检查用户是否领养了该宠物
     * GET /adoptions/check-ownership?petId=xxx&userId=xxx
     *
     * @param petId 宠物ID
     * @param userId 用户ID
     * @return 是否领养
     */
    @GetMapping("/check-ownership")
    ApiResponse<Boolean> checkOwnership(
            @RequestParam("petId") Long petId,
            @RequestParam("userId") Long userId);

    /**
     * 获取已领养宠物列表（用于查询逾期未更新）
     * GET /adoptions/adopted-pets?orgId=xxx&daysSinceUpdate=xxx
     *
     * @param orgId 机构ID（可选）
     * @param daysSinceUpdate 距离上次更新多少天（可选，默认30天）
     * @return 已领养宠物列表
     */
    @GetMapping("/adopted-pets")
    ApiResponse<List<Map<String, Object>>> getAdoptedPetsForReminder(
            @RequestParam(value = "orgId", required = false) Long orgId,
            @RequestParam(value = "daysSinceUpdate", defaultValue = "30") Integer daysSinceUpdate);
}

