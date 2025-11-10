package com.adoption.ai.feign;

import com.adoption.common.api.ApiResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import java.util.Map;

/**
 * Feign 客户端：调用 community-service
 *
 * 用于 AI 服务回调更新帖子状态
 */
@FeignClient(name = "community-service", path = "/community/posts")
public interface CommunityServiceClient {
    /**
     * 更新帖子 AI 标记状态
     */
    @PostMapping("/ai/update-flagged")
    ApiResponse<String> updatePostAiStatus(@RequestBody Map<String, Object> request);

    /**
     * 更新帖子 AI 总结
     */
    @PostMapping("/ai/update-summary")
    ApiResponse<String> updatePostAiSummary(@RequestBody Map<String, Object> request);
}

