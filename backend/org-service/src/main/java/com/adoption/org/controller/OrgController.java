package com.adoption.org.controller;

import com.adoption.common.api.ApiResponse;
import com.adoption.org.dto.OrganizationApplyRequest;
import com.adoption.org.dto.OrganizationApproveRequest;
import com.adoption.org.dto.AddMemberRequest;
import com.adoption.org.service.OrgService;
import org.springframework.web.bind.annotation.*;
import org.springframework.validation.annotation.Validated;
import jakarta.validation.Valid;

/**
 * OrgController
 * 本Controller负责机构组织相关的所有对外REST接口
 * 包含机构入驻申请、机构审核、机构成员管理等能力
 * 所有接口返回值均为 ApiResponse<T>，统一响应规范
 */
@RestController
@Validated
@RequestMapping("/org")
public class OrgController {

    // Service层依赖，通过构造方法注入
    private final OrgService orgService;

    public OrgController(OrgService orgService) {
        this.orgService = orgService;
    }

    /**
     * 机构入驻申请
     * POST /orgs/apply
     * 前端提交机构相关基础信息
     */
    @PostMapping("/apply")
    public ApiResponse<String> apply(
            @Valid @RequestBody OrganizationApplyRequest request,
            @RequestHeader("X-User-Id") String userId
    ) {
        return orgService.apply(request, userId);
    }


    /**
     * 管理员审核机构入驻申请（通过）
     * POST /orgs/{id}/approve
     */
    @PostMapping("/{id}/approve")
    public ApiResponse<String> approve(@PathVariable Long id, @Valid @RequestBody OrganizationApproveRequest request) {
        return orgService.approve(id, request);
    }

    /**
     * 管理员审核机构入驻申请（拒绝）
     * POST /orgs/{id}/reject
     */
    @PostMapping("/{id}/reject")
    public ApiResponse<String> reject(@PathVariable Long id, @Valid @RequestBody OrganizationApproveRequest request) {
        return orgService.reject(id, request);
    }

    /**
     * 查询机构详情
     * GET /orgs/{id}
     */
    @GetMapping("/{id}")
    public ApiResponse<Object> getDetail(@PathVariable Long id) {
        return orgService.getDetail(id);
    }

    /**
     * 给机构添加成员（机构管理员操作）
     * POST /orgs/{id}/members
     */
    @PostMapping("/{id}/members")
    public ApiResponse<String> addMember(@PathVariable Long id, @Valid @RequestBody AddMemberRequest request) {
        return orgService.addMember(id, request);
    }

    /**
     * 从机构中移除成员（机构管理员操作）
     * DELETE /orgs/{id}/members/{uid}
     */
    @DeleteMapping("/{id}/members/{uid}")
    public ApiResponse<String> deleteMember(@PathVariable Long id, @PathVariable Long uid) {
        return orgService.deleteMember(id, uid);
    }

    /**
     * 获取机构成员列表
     * GET /orgs/{id}/members
     */
    @GetMapping("/{id}/members")
    public ApiResponse<Object> getMembers(@PathVariable Long id) {
        return orgService.getMembers(id);
    }
}
