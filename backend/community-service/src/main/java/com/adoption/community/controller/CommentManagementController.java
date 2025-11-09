package com.adoption.community.controller;

import com.adoption.common.api.ApiResponse;
import com.adoption.common.util.UserContext;
import com.adoption.community.service.CommentService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

/**
 * 评论管理控制器
 * 
 * 作用：处理用户对自己评论的管理操作
 * 
 * 主要功能：
 * - 删除自己的评论
 * 
 * 注意：只能删除自己发布的评论，通过Service层验证权限
 */
@RestController
@RequestMapping("/comments")
public class CommentManagementController {

    @Autowired
    private CommentService commentService;

    @Autowired
    private UserContext userContext;

    /**
     * 删除自己的评论
     * DELETE /comments/{id}
     */
    @DeleteMapping("/{id}")
    public ApiResponse<String> deleteComment(@PathVariable("id") Long id) {
        Long currentUserId = userContext.getCurrentUserId();
        if (currentUserId == null) {
            return ApiResponse.error(401, "用户未登录");
        }
        return commentService.deleteComment(id, currentUserId);
    }
}

