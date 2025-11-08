package com.adoption.adoption.controller;

import com.adoption.adoption.feign.OrgServiceClient;
import com.adoption.adoption.model.AdoptionApp;
import com.adoption.adoption.model.AdoptionDoc;
import com.adoption.adoption.model.InterviewRecord;
import com.adoption.adoption.service.AdoptionService;
import com.adoption.common.api.ApiResponse;
import com.adoption.common.service.FileService;
import com.adoption.common.util.FileUtils;
import com.adoption.common.util.UserContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/adoptions")
public class AdoptionController {
    @Autowired
    private AdoptionService adoptionService;

    @Autowired
    private UserContext userContext; // 从 UserContext 获取当前用户信息

    @Autowired
    private FileService fileService; // 文件服务

    @Autowired
    private OrgServiceClient orgServiceClient; // 机构服务客户端

    /**
     * 获取当前用户所属的机构ID
     * @return 机构ID，如果用户不属于任何机构则返回null
     */
    private Long getCurrentUserOrgId() {
        Long currentUserId = userContext.getCurrentUserId();
        if (currentUserId == null) {
            return null;
        }

        try {
            ApiResponse<Object> membershipsResponse = orgServiceClient.getMemberships(currentUserId);
            if (membershipsResponse == null || membershipsResponse.getCode() != 200) {
                return null;
            }

            Object data = membershipsResponse.getData();
            if (data instanceof List) {
                List<?> list = (List<?>) data;
                if (!list.isEmpty()) {
                    Object firstItem = list.get(0);
                    if (firstItem instanceof Map) {
                        Map<?, ?> map = (Map<?, ?>) firstItem;
                        // 从连表查询结果中获取机构ID（org表的id字段）
                        Object orgIdObj = map.get("id");
                        if (orgIdObj != null) {
                            return Long.valueOf(orgIdObj.toString());
                        }
                    }
                }
            }
        } catch (Exception e) {
            // 记录日志但不抛出异常
            System.err.println("获取用户机构ID失败: " + e.getMessage());
        }
        return null;
    }

    // 用户 - 提交领养申请（checked）
    @PostMapping
    public ApiResponse<AdoptionApp> submitAdoption(@RequestBody AdoptionApp adoptionApp) {
        // 从 UserContext 获取当前用户 ID 并设置到 adoptionApp 中
        Long currentUserId = userContext.getCurrentUserId();
        if (currentUserId == null) {
            return ApiResponse.error(401, "用户未登录");
        }
        adoptionApp.setApplicantId(currentUserId);
        try {
        AdoptionApp result = adoptionService.submitAdoption(adoptionApp);
        return ApiResponse.success(result);
        } catch (RuntimeException e) {
            return ApiResponse.error(400, e.getMessage());
        }
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

    // 获取申请材料列表
    @GetMapping("/{id}/docs")
    public ApiResponse<List<AdoptionDoc>> getApplicationDocs(@PathVariable("id") Long id) {
        List<AdoptionDoc> docs = adoptionService.getApplicationDocs(id);
        return ApiResponse.success(docs);
    }

    // 用户 - 查看已领养宠物（checked）
    @GetMapping("/me/adoptions/adopted")
    public ApiResponse<List<AdoptionApp>> getAdoptedPets() {
        Long currentUserId = userContext.getCurrentUserId();
        List<AdoptionApp> result = adoptionService.getAdoptedPets(currentUserId);
        return ApiResponse.success(result);
    }

    /**
     * 检查用户是否领养了该宠物（供pet-service调用）
     * GET /adoptions/check-ownership?petId=xxx&userId=xxx
     */
    @GetMapping("/check-ownership")
    public ApiResponse<Boolean> checkOwnership(
            @RequestParam("petId") Long petId,
            @RequestParam("userId") Long userId) {
        boolean isOwner = adoptionService.checkOwnership(petId, userId);
        return ApiResponse.success(isOwner);
    }

    /**
     * 获取已领养宠物列表（用于查询逾期未更新，供pet-service调用）
     * GET /adoptions/adopted-pets?orgId=xxx&daysSinceUpdate=xxx
     */
    @GetMapping("/adopted-pets")
    public ApiResponse<List<Map<String, Object>>> getAdoptedPetsForReminder(
            @RequestParam(value = "orgId", required = false) Long orgId,
            @RequestParam(value = "daysSinceUpdate", defaultValue = "30") Integer daysSinceUpdate) {
        List<Map<String, Object>> result = adoptionService.getAdoptedPetsForReminder(orgId, daysSinceUpdate);
        return ApiResponse.success(result);
    }

    // 机构管理员 - 查看待审核申请（checked）
    @GetMapping("/org/adoptions")
    public ApiResponse<List<AdoptionApp>> getPendingApplications(@RequestParam(value = "status", required = false) String status) {
        Long orgId = getCurrentUserOrgId();
        if (orgId == null) {
            return ApiResponse.error(403, "您不属于任何机构，无法查看申请");
        }

        List<AdoptionApp> result = adoptionService.getPendingApplications(orgId, status);
        return ApiResponse.success(result);
    }

    // 机构管理员 - 初审申请通过（checked）
    @PostMapping("/{id}/approve")
    public ApiResponse<String> approveApplication(@PathVariable("id") Long id) {
        Long orgId = getCurrentUserOrgId();
        if (orgId == null) {
            return ApiResponse.error(403, "您不属于任何机构，无法操作");
        }
        boolean success = adoptionService.approveApplication(id, orgId);
        if (success) {
            return ApiResponse.success("申请已批准");
        } else {
            return ApiResponse.error(500, "申请批准失败");
        }
    }

    // 机构管理员 - 初审申请拒绝（checked）
    @PostMapping("/{id}/reject")
    public ApiResponse<String> rejectApplication(@PathVariable("id") Long id, @RequestBody String rejectReason) {
        Long orgId = getCurrentUserOrgId();
        if (orgId == null) {
            return ApiResponse.error(403, "您不属于任何机构，无法操作");
        }
        boolean success = adoptionService.rejectApplication(id, orgId, rejectReason);
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
        Long orgId = getCurrentUserOrgId();
        if (orgId == null) {
            return ApiResponse.error(403, "您不属于任何机构，无法操作");
        }
        boolean success = adoptionService.completeHandover(id, orgId);
        if (success) {
            return ApiResponse.success("交接已完成");
        } else {
            return ApiResponse.error(500, "交接完成失败");
        }
    }

    // 用户 - 上传领养资料材料
    // POST /adoptions/me/profile/upload
    // 参数: file (文件), docType (材料类型：ID_CARD, INCOME_PROOF, PET_HISTORY 等)
    @PostMapping("/me/profile/upload")
    public ApiResponse<Map<String, Object>> uploadProfileDoc(
            @RequestParam("file") MultipartFile file,
            @RequestParam("docType") String docType) {
        try {
            Long currentUserId = userContext.getCurrentUserId();

            if (file == null || file.isEmpty()) {
                return ApiResponse.error(400, "文件不能为空");
            }

            String filename = file.getOriginalFilename();
            // 允许图片和PDF文件
            if (!FileUtils.isImage(filename) && !filename.toLowerCase().endsWith(".pdf")) {
                return ApiResponse.error(400, "仅支持图片和PDF文件");
            }

            InputStream inputStream = FileUtils.toInputStream(file);
            if (inputStream == null) {
                return ApiResponse.error(400, "无法读取文件内容");
            }

            FileService.FileInfo fileInfo = fileService.uploadFile(inputStream, filename, "adoption-profile");

            AdoptionDoc doc = adoptionService.uploadUserProfileDoc(currentUserId, docType, fileInfo.getRelativePath());

            Map<String, Object> result = new HashMap<>();
            result.put("id", doc.getId());
            result.put("docType", docType);
            result.put("url", fileInfo.getUrl());
            result.put("relativePath", fileInfo.getRelativePath());
            result.put("originalFilename", fileInfo.getOriginalFilename());
            return ApiResponse.success(result);

        } catch (Exception e) {
            return ApiResponse.error(500, "文件上传失败: " + e.getMessage());
        }
    }

    // 用户 - 获取领养资料
    // GET /adoptions/me/profile
    @GetMapping("/me/profile")
    public ApiResponse<List<AdoptionDoc>> getUserProfile() {
        Long currentUserId = userContext.getCurrentUserId();
        List<AdoptionDoc> docs = adoptionService.getUserProfileDocs(currentUserId);
        // 处理 docType，去掉 USER_{userId}_ 前缀
        docs.forEach(doc -> {
            String docType = doc.getDocType();
            if (docType != null && docType.startsWith("USER_" + currentUserId + "_")) {
                doc.setDocType(docType.substring(("USER_" + currentUserId + "_").length()));
            }
        });
        return ApiResponse.success(docs);
    }

    // 用户 - 删除领养资料中的某个材料
    // DELETE /adoptions/me/profile/{docId}
    @DeleteMapping("/me/profile/{docId}")
    public ApiResponse<String> deleteProfileDoc(@PathVariable("docId") Long docId) {
        boolean success = adoptionService.deleteUserProfileDoc(docId);
        if (success) {
            return ApiResponse.success("材料已删除");
        } else {
            return ApiResponse.error(500, "删除失败");
        }
    }

    // 用户 - 检查是否填写了领养资料
    // GET /adoptions/me/profile/check
    @GetMapping("/me/profile/check")
    public ApiResponse<Map<String, Object>> checkUserProfile() {
        Long currentUserId = userContext.getCurrentUserId();
        boolean hasProfile = adoptionService.hasUserProfile(currentUserId);
        Map<String, Object> result = new HashMap<>();
        result.put("hasProfile", hasProfile);
        return ApiResponse.success(result);
    }

    /**
     * 发送面谈确认通知给申请人（供 interview-service 调用）
     * POST /adoptions/{id}/notify/interview-confirmed
     * C端用例：注册用户收到系统通知 -> 面谈时间提醒
     */
    @PostMapping("/{id}/notify/interview-confirmed")
    public ApiResponse<String> notifyInterviewConfirmed(
            @PathVariable("id") Long appId,
            @RequestBody Map<String, String> body) {
        String interviewTime = body.get("interviewTime");
        String interviewLocation = body.get("interviewLocation");

        if (interviewTime == null || interviewTime.isEmpty()) {
            return ApiResponse.error(400, "面谈时间不能为空");
        }

        adoptionService.notifyInterviewConfirmed(appId, interviewTime, interviewLocation);
        return ApiResponse.success("面谈确认通知已发送");
    }

    /**
     * 发送面谈预约请求通知给机构管理员（供 interview-service 调用）
     * POST /adoptions/{id}/notify/interview-requested
     */
    @PostMapping("/{id}/notify/interview-requested")
    public ApiResponse<String> notifyInterviewRequested(@PathVariable("id") Long appId) {
        adoptionService.notifyInterviewRequested(appId);
        return ApiResponse.success("面谈预约请求通知已发送");
    }
}