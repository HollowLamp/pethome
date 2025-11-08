package com.adoption.interview.controller;

import com.adoption.common.api.ApiResponse;
import com.adoption.common.util.UserContext;
import com.adoption.interview.feign.OrgServiceClient;
import com.adoption.interview.model.InterviewBooking;
import com.adoption.interview.service.InterviewService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/interview")
public class InterviewController {

    @Autowired
    private InterviewService interviewService;

    @Autowired
    private UserContext userContext;

    @Autowired
    private OrgServiceClient orgServiceClient;

    /**
     * 获取当前用户所属的机构ID
     */
    private Long getCurrentUserOrgId() {
        Long currentUserId = userContext.getCurrentUserId();
        if (currentUserId == null) {
            return null;
        }

        try {
            com.adoption.common.api.ApiResponse<Object> membershipsResponse = orgServiceClient.getMemberships(currentUserId);
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
                        Object orgIdObj = map.get("id");
                        if (orgIdObj != null) {
                            return Long.valueOf(orgIdObj.toString());
                        }
                    }
                }
            }
        } catch (Exception e) {
            System.err.println("获取用户机构ID失败: " + e.getMessage());
        }
        return null;
    }

    /**
     * 用户提交面谈预约请求
     * POST /interview/adoptions/{id}/interview/request
     */
    @PostMapping("/adoptions/{id}/interview/request")
    public ApiResponse<InterviewBooking> requestInterview(
            @PathVariable("id") Long appId,
            @RequestBody Map<String, Long> body) {
        Long currentUserId = userContext.getCurrentUserId();
        if (currentUserId == null) {
            return ApiResponse.error(401, "用户未登录");
        }

        Long slotId = body.get("slotId");
        if (slotId == null) {
            return ApiResponse.error(400, "请选择面谈时段");
        }

        return interviewService.requestInterview(appId, slotId);
    }

    /**
     * 机构管理员查看预约请求
     * GET /interview/adoptions/{id}/interview
     */
    @GetMapping("/adoptions/{id}/interview")
    public ApiResponse<InterviewBooking> getInterviewRequests(@PathVariable("id") Long appId) {
        Long orgId = getCurrentUserOrgId();
        if (orgId == null) {
            return ApiResponse.error(403, "您不属于任何机构，无法查看预约请求");
        }

        return interviewService.getInterviewByAppId(appId);
    }

    /**
     * 机构管理员确认面谈
     * POST /interview/adoptions/{id}/interview/confirm
     */
    @PostMapping("/adoptions/{id}/interview/confirm")
    public ApiResponse<String> confirmInterview(@PathVariable("id") Long appId) {
        Long orgId = getCurrentUserOrgId();
        if (orgId == null) {
            return ApiResponse.error(403, "您不属于任何机构，无法操作");
        }

        return interviewService.confirmInterview(appId);
    }

    /**
     * 机构管理员完成面谈
     * POST /interview/adoptions/{id}/interview/complete
     */
    @PostMapping("/adoptions/{id}/interview/complete")
    public ApiResponse<String> completeInterview(@PathVariable("id") Long appId) {
        Long orgId = getCurrentUserOrgId();
        if (orgId == null) {
            return ApiResponse.error(403, "您不属于任何机构，无法操作");
        }

        return interviewService.completeInterview(appId);
    }

    /**
     * 机构管理员完成交接
     * POST /interview/adoptions/{id}/handover/complete
     */
    @PostMapping("/adoptions/{id}/handover/complete")
    public ApiResponse<String> completeHandover(@PathVariable("id") Long appId) {
        Long orgId = getCurrentUserOrgId();
        if (orgId == null) {
            return ApiResponse.error(403, "您不属于任何机构，无法操作");
        }

        return interviewService.completeHandover(appId);
    }
}

