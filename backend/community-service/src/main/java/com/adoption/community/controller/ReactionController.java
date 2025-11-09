package com.adoption.community.controller;

import com.adoption.common.api.ApiResponse;
import com.adoption.common.util.UserContext;
import com.adoption.community.service.ReactionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * 互动反应控制器
 *
 * 作用：处理点赞等互动行为的HTTP请求
 *
 * 主要功能：
 * - 点赞/取消点赞帖子（幂等操作）
 * - 点赞/取消点赞评论（幂等操作）
 *
 * 注意：点赞后，可以考虑发送通知给帖子/评论作者（通过NotificationMessageService）
 */
@RestController
@RequestMapping("/community")
public class ReactionController {

    @Autowired
    private ReactionService reactionService;

    @Autowired
    private UserContext userContext;

    /**
     * 点赞/取消点赞帖子（幂等）
     * POST /posts/{id}/like
     */
    @PostMapping("/posts/{id}/like")
    public ApiResponse<Map<String, Object>> togglePostLike(@PathVariable("id") Long postId) {
        Long currentUserId = userContext.getCurrentUserId();
        if (currentUserId == null) {
            return ApiResponse.error(401, "用户未登录");
        }
        return reactionService.togglePostLike(postId, currentUserId);
    }

    /**
     * 点赞/取消点赞评论（幂等）
     * POST /comments/{id}/like
     */
    @PostMapping("/comments/{id}/like")
    public ApiResponse<Map<String, Object>> toggleCommentLike(@PathVariable("id") Long commentId) {
        Long currentUserId = userContext.getCurrentUserId();
        if (currentUserId == null) {
            return ApiResponse.error(401, "用户未登录");
        }
        return reactionService.toggleCommentLike(commentId, currentUserId);
    }
}

