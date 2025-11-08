package com.adoption.interview.feign;

import com.adoption.common.api.ApiResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@FeignClient(name = "org-service", path = "/org")
public interface OrgServiceClient {
    @GetMapping("/users/{uid}/memberships")
    ApiResponse<Object> getMemberships(@PathVariable("uid") Long userId);
}

