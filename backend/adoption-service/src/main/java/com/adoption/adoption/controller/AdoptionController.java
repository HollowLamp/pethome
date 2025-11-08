package com.adoption.adoption.controller;

import com.adoption.adoption.model.AdoptionApp;
import com.adoption.adoption.model.InterviewRecord;
import com.adoption.adoption.service.AdoptionService;
import com.adoption.common.api.ApiResponse;
import com.adoption.common.util.UserContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/adoptions")
public class AdoptionController {
    @Autowired
    private AdoptionService adoptionService;
    
    @Autowired
    private UserContext userContext; // 从 UserContext 获取当前用户信息

    // 用户 - 提交领养申请（checked）
    @PostMapping
    public ApiResponse<AdoptionApp> submitAdoption(@RequestBody AdoptionApp adoptionApp) {
        AdoptionApp result = adoptionService.submitAdoption(adoptionApp);
        return ApiResponse.success(result);
    }

    // 用户 - 查看我的申请（checked）
    @GetMapping("/me/adoptions")
    public ApiResponse<List<AdoptionApp>> getMyApplications() {
        Long currentUserId = userContext.getCurrentUserId();
        List<AdoptionApp> result = adoptionService.getMyApplications(currentUserId);
        return ApiResponse.success(result);
    }

    // 用户 - 查看申请详情（checked）
    @GetMapping("/{id}")
    public ApiResponse<AdoptionApp> getApplicationDetail(@PathVariable("id") Long id) {
        System.out.println(id); // 测试api中的id是否正确
        AdoptionApp result = adoptionService.getApplicationDetail(id);
        return ApiResponse.success(result);
    }

    // 用户 - 查看已领养宠物（checked）
    @GetMapping("/me/adoptions/adopted")
    public ApiResponse<List<AdoptionApp>> getAdoptedPets() {
        Long currentUserId = userContext.getCurrentUserId();
        List<AdoptionApp> result = adoptionService.getAdoptedPets(currentUserId);
        return ApiResponse.success(result);
    }

    // 机构管理员 - 查看待审核申请（checked）
    @GetMapping("/org/adoptions")
    public ApiResponse<List<AdoptionApp>> getPendingApplications(@RequestParam(required = false) String status) {
        Long currentOrgId = userContext.getCurrentUserId();
        List<AdoptionApp> result = adoptionService.getPendingApplications(currentOrgId, status);
        return ApiResponse.success(result);
    }

    // 机构管理员 - 初审申请通过（checked）
    @PostMapping("/{id}/approve")
    public ApiResponse<String> approveApplication(@PathVariable("id") Long id) {
        Long currentOrgId = userContext.getCurrentUserId();
        boolean success = adoptionService.approveApplication(id, currentOrgId);
        if (success) {
            return ApiResponse.success("申请已批准");
        } else {
            return ApiResponse.error(500, "申请批准失败");
        }
    }

    // 机构管理员 - 初审申请拒绝（checked）
    @PostMapping("/{id}/reject")
    public ApiResponse<String> rejectApplication(@PathVariable("id") Long id, @RequestBody String rejectReason) {
        Long currentOrgId = userContext.getCurrentUserId();
        boolean success = adoptionService.rejectApplication(id, currentOrgId, rejectReason);
        if (success) {
            return ApiResponse.success("申请已拒绝");
        } else {
            return ApiResponse.error(500, "申请拒绝失败");
        }
    }

    // 审核员 - 复审申请 (批准)
    @PostMapping("/{id}/platform-approve")
    public ApiResponse<String> platformApproveApplication(@PathVariable("id") Long id) {
        boolean success = adoptionService.platformApproveApplication(id);
        if (success) {
            return ApiResponse.success("申请平台审核已批准");
        } else {
            return ApiResponse.error(500, "申请平台审核批准失败");
        }
    }

    // 审核员 - 复审申请 (拒绝)
    @PostMapping("/{id}/platform-reject")
    public ApiResponse<String> platformRejectApplication(@PathVariable("id") Long id, @RequestBody String rejectReason) {
        boolean success = adoptionService.platformRejectApplication(id, rejectReason);
        if (success) {
            return ApiResponse.success("申请平台审核已拒绝");
        } else {
            return ApiResponse.error(500, "申请平台审核拒绝失败");
        }
    }

    // 机构管理员 - 确认交接完成
    @PostMapping("/{id}/handover/complete")
    public ApiResponse<String> completeHandover(@PathVariable("id") Long id) {
        Long currentOrgId = userContext.getCurrentUserId();
        boolean success = adoptionService.completeHandover(id, currentOrgId);
        if (success) {
            return ApiResponse.success("交接已完成");
        } else {
            return ApiResponse.error(500, "交接完成失败");
        }
    }
}