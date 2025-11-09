package com.adoption.community.controller;

import com.adoption.common.api.ApiResponse;
import com.adoption.common.util.UserContext;
import com.adoption.community.service.PostService;
import com.adoption.community.service.ReportService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * 管理员控制器
 * 
 * 作用：处理管理员和客服相关的HTTP请求
 * 
 * 主要功能：
 * - 客服功能：
 *   * 获取AI标记的违规帖子列表
 *   * 修改帖子状态（审核违规帖子）
 *   * 获取举报列表
 *   * 处理举报
 * - 管理员功能：
 *   * 推荐/取消推荐帖子
 * 
 * 权限要求：
 * - 客服功能需要CS（客服）角色
 * - 推荐功能需要ADMIN（超级管理员）角色
 * 
 * 注意：目前权限验证为TODO，需要在网关或拦截器中实现
 */

@RestController
public class AdminController {

    @Autowired
    private PostService postService;

    @Autowired
    private ReportService reportService;

    @Autowired
    private UserContext userContext;

    /**
     * 获取AI标记的违规帖子（客服）
     * GET /posts/flagged
     */
    @GetMapping("/posts/flagged")
    public ApiResponse<Map<String, Object>> getFlaggedPosts(
            @RequestParam(name = "page", required = false) Integer page,
            @RequestParam(name = "pageSize", required = false) Integer pageSize) {
        // TODO: 验证用户是否为客服角色
        Long currentUserId = userContext.getCurrentUserId();
        if (currentUserId == null) {
            return ApiResponse.error(401, "用户未登录");
        }
        return postService.getFlaggedPosts(page, pageSize);
    }

    /**
     * 修改帖子状态（客服）
     * PATCH /posts/{id}/status
     */
    @PatchMapping("/posts/{id}/status")
    public ApiResponse<String> updatePostStatus(
            @PathVariable("id") Long id,
            @RequestBody Map<String, String> body) {
        // TODO: 验证用户是否为客服角色
        Long currentUserId = userContext.getCurrentUserId();
        if (currentUserId == null) {
            return ApiResponse.error(401, "用户未登录");
        }
        String status = body.get("status");
        if (status == null) {
            return ApiResponse.error(400, "状态不能为空");
        }
        return postService.updatePostStatus(id, status);
    }

    /**
     * 获取举报列表（客服）
     * GET /reports
     */
    @GetMapping("/reports")
    public ApiResponse<Map<String, Object>> getReports(
            @RequestParam(name = "status", required = false) String status,
            @RequestParam(name = "page", required = false) Integer page,
            @RequestParam(name = "pageSize", required = false) Integer pageSize) {
        // TODO: 验证用户是否为客服角色
        Long currentUserId = userContext.getCurrentUserId();
        if (currentUserId == null) {
            return ApiResponse.error(401, "用户未登录");
        }
        return reportService.getReports(status, page, pageSize);
    }

    /**
     * 处理举报（客服）
     * PATCH /reports/{id}/status
     */
    @PatchMapping("/reports/{id}/status")
    public ApiResponse<String> handleReport(
            @PathVariable("id") Long id,
            @RequestBody Map<String, String> body) {
        // TODO: 验证用户是否为客服角色
        Long currentUserId = userContext.getCurrentUserId();
        if (currentUserId == null) {
            return ApiResponse.error(401, "用户未登录");
        }
        String status = body.get("status");
        if (status == null) {
            return ApiResponse.error(400, "状态不能为空");
        }
        return reportService.handleReport(id, status, currentUserId);
    }

    /**
     * 推荐/取消推荐帖子（超级管理员）
     * POST /posts/{id}/recommend
     */
    @PostMapping("/posts/{id}/recommend")
    public ApiResponse<String> toggleRecommend(
            @PathVariable("id") Long id,
            @RequestBody(required = false) Map<String, Boolean> body) {
        // TODO: 验证用户是否为超级管理员角色
        Long currentUserId = userContext.getCurrentUserId();
        if (currentUserId == null) {
            return ApiResponse.error(401, "用户未登录");
        }
        Boolean recommend = body != null ? body.get("recommend") : null;
        return postService.toggleRecommend(id, recommend);
    }
}

