package com.adoption.org.controller;

import com.adoption.common.api.ApiResponse;
import com.adoption.org.dto.OrganizationApplyRequest;
import com.adoption.org.dto.OrganizationApproveRequest;
import com.adoption.org.dto.AddMemberRequest;
import com.adoption.org.service.OrgService;
import com.adoption.common.service.FileService;
import com.adoption.common.util.FileUtils;
import org.springframework.web.bind.annotation.*;
import org.springframework.validation.annotation.Validated;
import jakarta.validation.Valid;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

/**
 * OrgController
 *
 * 说明：
 * - 对外提供“机构组织”相关 REST 接口。
 * - 涵盖：机构入驻申请、审核（通过/拒绝）、机构成员的增删查、机构详情等。
 * - 统一返回体：ApiResponse<T>，便于网关与前端统一处理。
 *
 * 接口鉴别：
 * - 本服务通过请求头 X-User-Id 获取当前用户身份（由网关透传）。
 * - 更复杂的角色/权限判断，请在 Service 层完成，Controller 只做参数接收与转发。
 */
@RestController
@Validated
@RequestMapping("/org")
public class OrgController {

    // Service 层依赖，通过构造方法注入，便于测试与解耦
    private final OrgService orgService;
    private final FileService fileService;

    public OrgController(OrgService orgService, FileService fileService) {
        this.orgService = orgService;
        this.fileService = fileService;
    }

    /**
     * 机构入驻申请
     * Method: POST /org/apply
     * 入参：OrganizationApplyRequest（机构基本资料）+ X-User-Id（申请人用户ID）
     * 说明：将新机构以 PENDING 状态入库，等待管理员审核
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
     * Method: POST /org/{id}/approve
     * 说明：
     * - 仅允许对 PENDING 状态的机构进行通过操作
     * - 通过后将创建者加入 org_member，避免后续无人管理
     */
    @PostMapping("/{id}/approve")
    public ApiResponse<String> approve(@PathVariable("id") Long id, @Valid @RequestBody OrganizationApproveRequest request) {
        return orgService.approve(id, request);
    }

    /**
     * 管理员审核机构入驻申请（拒绝）
     * Method: POST /org/{id}/reject
     * 说明：仅允许对 PENDING 状态的机构进行拒绝操作
     */
    @PostMapping("/{id}/reject")
    public ApiResponse<String> reject(@PathVariable("id") Long id, @Valid @RequestBody OrganizationApproveRequest request) {
        return orgService.reject(id, request);
    }

    /**
     * 查询机构详情
     * Method: GET /org/{id}
     * 返回：Organization 基础信息（后续可拼接成员列表/统计等）
     */
    @GetMapping("/{id}")
    public ApiResponse<Object> getDetail(@PathVariable("id") Long id) {
        return orgService.getDetail(id);
    }

    /**
     * 给机构添加成员（机构拥有者操作）
     * Method: POST /org/{id}/members
     * 入参：AddMemberRequest(userId)
     * 说明：若成员已存在则返回 400，避免重复
     */
    @PostMapping("/{id}/members")
    public ApiResponse<String> addMember(@PathVariable("id") Long id, @Valid @RequestBody AddMemberRequest request) {
        return orgService.addMember(id, request);
    }

    /**
     * 从机构中移除成员（机构拥有者操作）
     * Method: DELETE /org/{id}/members/{uid}
     */
    @DeleteMapping("/{id}/members/{uid}")
    public ApiResponse<String> deleteMember(@PathVariable("id") Long id, @PathVariable("uid") Long uid) {
        return orgService.deleteMember(id, uid);
    }

    /**
     * 获取机构成员列表
     * Method: GET /org/{id}/members
     */
    @GetMapping("/{id}/members")
    public ApiResponse<Object> getMembers(@PathVariable("id") Long id) {
        return orgService.getMembers(id);
    }

    /**
     * 查询用户加入的机构列表
     * Method: GET /org/users/{uid}/memberships
     */
    @GetMapping("/users/{uid}/memberships")
    public ApiResponse<Object> getMemberships(@PathVariable("uid") Long userId) {
        return orgService.getMemberships(userId);
    }

    /**
     * 上传机构资质文件（支持申请前或更新已有机构的资质）
     * POST /org/license/upload
     */
    @PostMapping("/license/upload")
    public ApiResponse<Map<String, Object>> uploadLicense(
            @RequestHeader("X-User-Id") Long userId,
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "orgId", required = false) Long orgId
    ) {
        try {
            if (file == null || file.isEmpty()) {
                return ApiResponse.error(400, "文件不能为空");
            }

            String filename = file.getOriginalFilename();
            if (!FileUtils.isImage(filename)) {
                return ApiResponse.error(400, "仅支持图片文件");
            }

            InputStream inputStream = FileUtils.toInputStream(file);
            if (inputStream == null) {
                return ApiResponse.error(400, "无法读取文件内容");
            }

            FileService.FileInfo fileInfo = fileService.uploadFile(inputStream, filename, "org-license");

            if (orgId != null) {
                ApiResponse<?> updateRes = orgService.updateLicense(orgId, userId, fileInfo.getRelativePath());
                if (updateRes.getCode() != 200) {
                    return ApiResponse.error(updateRes.getCode(), updateRes.getMessage());
                }
            }

            Map<String, Object> result = new HashMap<>();
            result.put("relativePath", fileInfo.getRelativePath());
            result.put("licenseUrl", fileInfo.getUrl());
            result.put("originalFilename", fileInfo.getOriginalFilename());
            return ApiResponse.success(result);

        } catch (Exception e) {
            return ApiResponse.error(500, "资质文件上传失败: " + e.getMessage());
        }
    }

    /**
     * 获取待审核的机构列表
     */
    @GetMapping("/applications/pending")
    public ApiResponse<java.util.List<com.adoption.org.entity.Organization>> listPending() {
        return orgService.listPendingOrganizations();
    }
}
