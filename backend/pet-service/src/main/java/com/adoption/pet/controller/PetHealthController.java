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

    /**
     * C端用户上传健康状态（需要验证用户是否领养了该宠物）
     * POST /pets/{id}/health/owner
     */
    @PostMapping("/{id}/health/owner")
    public ApiResponse<PetHealth> updateHealthByOwner(
            @RequestHeader("X-User-Id") Long userId,
            @PathVariable("id") Long petId,
            @RequestBody PetHealth health) {
        return petHealthService.updateHealthByOwner(petId, health, userId);
    }

    /**
     * 查询逾期未更新健康状态的宠物（B端用）
     * GET /pets/health/overdue?orgId=xxx&daysSinceUpdate=xxx
     */
    @GetMapping("/health/overdue")
    public ApiResponse<List<Map<String, Object>>> getOverduePets(
            @RequestParam(value = "orgId", required = false) Long orgId,
            @RequestParam(value = "daysSinceUpdate", defaultValue = "30") Integer daysSinceUpdate) {
        return petHealthService.getOverduePets(orgId, daysSinceUpdate);
    }

    /**
     * 发送逾期提醒通知给宠物主人（B端用）
     * POST /pets/health/remind-overdue
     */
    @PostMapping("/health/remind-overdue")
    public ApiResponse<String> sendOverdueReminder(@RequestBody Map<String, Object> body) {
        Object petIdObj = body.get("petId");
        Object applicantIdObj = body.get("applicantId");
        Object daysOverdueObj = body.get("daysOverdue");

        if (petIdObj == null || applicantIdObj == null || daysOverdueObj == null) {
            return ApiResponse.error(400, "参数不完整");
        }

        Long petId = Long.valueOf(petIdObj.toString());
        Long applicantId = Long.valueOf(applicantIdObj.toString());
        Long daysOverdue = Long.valueOf(daysOverdueObj.toString());

        return petHealthService.sendOverdueReminder(petId, applicantId, daysOverdue);
    }
}

