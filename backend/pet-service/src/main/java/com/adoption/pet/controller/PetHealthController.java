package com.adoption.pet.controller;

import com.adoption.common.api.ApiResponse;
import com.adoption.pet.model.PetHealth;
import com.adoption.pet.service.PetHealthService;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 宠物健康记录Controller
 * 维护员可访问
 */
@RestController
@RequestMapping("/pets")
public class PetHealthController {
    private final PetHealthService petHealthService;

    public PetHealthController(PetHealthService petHealthService) {
        this.petHealthService = petHealthService;
    }

    /**
     * 更新健康/疫苗记录（维护员）
     * POST /pets/{id}/health
     */
    @PostMapping("/{id}/health")
    public ApiResponse<PetHealth> updateHealth(
            @RequestHeader("X-User-Id") Long userId,
            @PathVariable("id") Long petId,
            @RequestBody PetHealth health) {
        return petHealthService.updateHealth(petId, health, userId);
    }

    /**
     * 获取健康记录
     * GET /pets/{id}/health
     */
    @GetMapping("/{id}/health")
    public ApiResponse<PetHealth> getHealth(@PathVariable("id") Long petId) {
        return petHealthService.getHealth(petId);
    }

    /**
     * 获取健康记录历史
     * GET /pets/{id}/health/history
     */
    @GetMapping("/{id}/health/history")
    public ApiResponse<List<PetHealth>> getHealthHistory(@PathVariable("id") Long petId) {
        return petHealthService.getHealthHistory(petId);
    }
}

