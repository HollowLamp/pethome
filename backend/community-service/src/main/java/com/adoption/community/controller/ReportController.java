package com.adoption.community.controller;

import com.adoption.common.api.ApiResponse;
import com.adoption.common.util.UserContext;
import com.adoption.community.model.Report;
import com.adoption.community.service.ReportService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

/**
 * 举报控制器
 * 
 * 作用：处理举报相关的HTTP请求
 * 
 * 主要功能：
 * - 举报帖子
 * - 举报评论
 * 
 * 注意：举报后，可以考虑发送通知给客服人员（通过NotificationMessageService）
 */
@RestController
public class ReportController {

    @Autowired
    private ReportService reportService;

    @Autowired
    private UserContext userContext;

    /**
     * 举报帖子
     * POST /posts/{id}/report
     */
    @PostMapping("/posts/{id}/report")
    public ApiResponse<Report> reportPost(
            @PathVariable("id") Long postId,
            @RequestBody Report report) {
        Long currentUserId = userContext.getCurrentUserId();
        if (currentUserId == null) {
            return ApiResponse.error(401, "用户未登录");
        }
        return reportService.reportPost(postId, report, currentUserId);
    }

    /**
     * 举报评论
     * POST /comments/{id}/report
     */
    @PostMapping("/comments/{id}/report")
    public ApiResponse<Report> reportComment(
            @PathVariable("id") Long commentId,
            @RequestBody Report report) {
        Long currentUserId = userContext.getCurrentUserId();
        if (currentUserId == null) {
            return ApiResponse.error(401, "用户未登录");
        }
        return reportService.reportComment(commentId, report, currentUserId);
    }
}

