package com.adoption.interview.feign;

import com.adoption.common.api.ApiResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestBody;

import java.util.Map;

/**
 * Feign 客户端：调用 adoption-service
 */
@FeignClient(name = "adoption-service", path = "/adoptions")
public interface AdoptionServiceClient {
    @PostMapping("/{id}/handover/complete")
    ApiResponse<String> completeHandover(@PathVariable("id") Long appId);

    @GetMapping("/{id}")
    ApiResponse<Object> getApplicationDetail(@PathVariable("id") Long appId);

    /**
     * 发送面谈确认通知给申请人
     *
     * @param appId 申请ID
     * @param body 请求体，包含 interviewTime 和 interviewLocation（可选）
     * @return 操作结果
     */
    @PostMapping("/{id}/notify/interview-confirmed")
    ApiResponse<String> notifyInterviewConfirmed(
            @PathVariable("id") Long appId,
            @RequestBody Map<String, String> body
    );

    /**
     * 发送面谈预约请求通知给机构管理员
     *
     * @param appId 申请ID
     * @return 操作结果
     */
    @PostMapping("/{id}/notify/interview-requested")
    ApiResponse<String> notifyInterviewRequested(@PathVariable("id") Long appId);
}

