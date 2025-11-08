package com.adoption.community.controller;

import com.adoption.common.api.ApiResponse;
import com.adoption.common.util.UserContext;
import com.adoption.community.model.Comment;
import com.adoption.community.service.CommentService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * 评论控制器
 * 
 * 作用：处理评论相关的HTTP请求
 * 
 * 主要功能：
 * - 获取评论列表（分页）
 * - 发布评论
 * 
 * 注意：评论发布后，可以考虑发送通知给帖子作者（通过NotificationMessageService）
 */
@RestController
@RequestMapping("/posts/{postId}/comments")
public class CommentController {

    @Autowired
    private CommentService commentService;

    @Autowired
    private UserContext userContext;

    /**
     * 获取评论列表
     * GET /posts/{id}/comments
     */
    @GetMapping
    public ApiResponse<Map<String, Object>> getComments(
            @PathVariable("postId") Long postId,
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer pageSize) {
        return commentService.getComments(postId, page, pageSize);
    }

    /**
     * 发布评论
     * POST /posts/{id}/comments
     */
    @PostMapping
    public ApiResponse<Comment> createComment(
            @PathVariable("postId") Long postId,
            @RequestBody Comment comment) {
        Long currentUserId = userContext.getCurrentUserId();
        if (currentUserId == null) {
            return ApiResponse.error(401, "用户未登录");
        }
        return commentService.createComment(postId, comment, currentUserId);
    }
}

