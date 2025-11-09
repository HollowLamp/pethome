package com.adoption.community.service;

import com.adoption.common.api.ApiResponse;
import com.adoption.community.model.Comment;
import com.adoption.community.model.Post;
import com.adoption.community.repository.CommentMapper;
import com.adoption.community.repository.PostMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 评论服务层
 * 
 * 作用：处理评论相关的业务逻辑
 * 
 * 主要功能：
 * - 获取评论列表（分页）
 * - 发布评论
 * - 删除评论
 * 
 * 注意：评论发布后，可以考虑发送通知给帖子作者（通过NotificationMessageService）
 */
@Service
public class CommentService {
    private final CommentMapper commentMapper;
    private final PostMapper postMapper;
    @Autowired
    private final NotificationMessageService notificationMessageService;

    public CommentService(CommentMapper commentMapper, PostMapper postMapper, NotificationMessageService notificationMessageService) {
        this.commentMapper = commentMapper;
        this.postMapper = postMapper;
        this.notificationMessageService = notificationMessageService;
    }

    /**
     * 获取评论列表（分页查询）
     * 
     * 功能说明：
     * - 只返回状态为VISIBLE（可见）的评论
     * - 按创建时间正序排列（最早发布的在前）
     * - 支持分页查询
     * 
     * @param postId 帖子ID
     * @param page 页码（从1开始，默认1）
     * @param pageSize 每页数量（默认20，最大100）
     * @return 包含评论列表、总数、页码等信息的响应
     */
    public ApiResponse<Map<String, Object>> getComments(Long postId, Integer page, Integer pageSize) {
        Post post = postMapper.findById(postId);
        if (post == null) {
            return ApiResponse.error(404, "帖子不存在");
        }

        if (page == null || page < 1) {
            page = 1;
        }
        if (pageSize == null || pageSize < 1) {
            pageSize = 20;
        }
        if (pageSize > 100) {
            pageSize = 100;
        }

        int offset = (page - 1) * pageSize;
        List<Comment> comments = commentMapper.findByPostId(postId, offset, pageSize);
        int total = commentMapper.countByPostId(postId);

        Map<String, Object> result = new HashMap<>();
        result.put("list", comments);
        result.put("total", total);
        result.put("page", page);
        result.put("pageSize", pageSize);

        return ApiResponse.success(result);
    }

    /**
     * 发布评论
     * 
     * 功能说明：
     * - 验证帖子存在且状态为PUBLISHED
     * - 验证评论内容不能为空
     * - 自动设置帖子ID、作者ID、默认状态为VISIBLE
     * - 插入数据库后返回包含ID的评论对象
     * - 发送通知给帖子作者
     * 
     * @param postId 帖子ID
     * @param comment 评论对象（需要包含content字段）
     * @param authorId 评论作者用户ID（从UserContext获取）
     * @return 创建成功的评论对象（包含自动生成的ID）
     */
    public ApiResponse<Comment> createComment(Long postId, Comment comment, Long authorId) {
        Post post = postMapper.findById(postId);
        if (post == null) {
            return ApiResponse.error(404, "帖子不存在");
        }
        if (!"PUBLISHED".equals(post.getStatus())) {
            return ApiResponse.error(400, "帖子已下架，无法评论");
        }

        if (comment.getContent() == null || comment.getContent().trim().isEmpty()) {
            return ApiResponse.error(400, "评论内容不能为空");
        }

        comment.setPostId(postId);
        comment.setAuthorId(authorId);
        if (comment.getStatus() == null) {
            comment.setStatus("VISIBLE");
        }

        commentMapper.insert(comment);
        
        // 发送通知给帖子作者
        try {
            notificationMessageService.sendSystemNotification(
                post.getAuthorId(),
                "您的帖子有新评论",
                String.format("用户%s评论了您的帖子《%s》", authorId, post.getTitle())
            );
        } catch (Exception e) {
            System.err.println("发送通知失败: " + e.getMessage());
        }
        
        return ApiResponse.success(comment);
    }

    /**
     * 删除自己的评论
     * 
     * 注意：只能删除自己发布的评论，通过同时验证id和authorId确保安全性
     * 
     * @param id 评论ID
     * @param authorId 作者用户ID（用于验证权限）
     * @return 删除结果
     */
    public ApiResponse<String> deleteComment(Long id, Long authorId) {
        Comment comment = commentMapper.findById(id);
        if (comment == null) {
            return ApiResponse.error(404, "评论不存在");
        }
        if (!comment.getAuthorId().equals(authorId)) {
            return ApiResponse.error(403, "只能删除自己的评论");
        }

        int deleted = commentMapper.deleteByIdAndAuthorId(id, authorId);
        if (deleted > 0) {
            return ApiResponse.success("删除成功");
        } else {
            return ApiResponse.error(500, "删除失败");
        }
    }
}

