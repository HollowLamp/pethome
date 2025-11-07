package com.adoption.pet.feign;

import com.adoption.common.api.ApiResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.Map;

/**
 * Feign 客户端：调用 org-service 获取机构信息
 */
@FeignClient(name = "org-service", path = "/org")
public interface OrgServiceClient {

    /**
     * 获取机构详情
     * @param orgId 机构ID
     * @return 机构信息
     */
    @GetMapping("/{id}")
    ApiResponse<Map<String, Object>> getOrgDetail(@PathVariable("id") Long orgId);
}

