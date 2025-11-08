package com.adoption.community.service;

import com.adoption.common.api.ApiResponse;
import com.adoption.community.model.Post;
import com.adoption.community.model.Comment;
import com.adoption.community.model.Reaction;
import com.adoption.community.repository.PostMapper;
import com.adoption.community.repository.CommentMapper;
import com.adoption.community.repository.ReactionMapper;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

/**
 * 互动反应服务层
 * 
 * 作用：处理点赞等互动行为的业务逻辑
 * 
 * 主要功能：
 * - 点赞/取消点赞帖子（幂等操作）
 * - 点赞/取消点赞评论（幂等操作）
 * 
 * 实现原理：
 * - 通过查询是否存在记录来判断是否已点赞
 * - 如果已点赞则删除记录（取消点赞），否则插入记录（点赞）
 * - 返回当前点赞状态和点赞总数
 * 
 * 扩展建议：可以在这里调用NotificationMessageService发送点赞通知
 */
@Service
public class ReactionService {
    private final ReactionMapper reactionMapper;
    private final PostMapper postMapper;
    private final CommentMapper commentMapper;

    public ReactionService(ReactionMapper reactionMapper, PostMapper postMapper, CommentMapper commentMapper) {
        this.reactionMapper = reactionMapper;
        this.postMapper = postMapper;
        this.commentMapper = commentMapper;
    }

    /**
     * 点赞/取消点赞帖子（幂等操作）
     * 
     * 功能说明：
     * - 验证帖子存在且状态为PUBLISHED
     * - 如果用户已点赞，则取消点赞（删除记录）
     * - 如果用户未点赞，则点赞（插入记录）
     * - 返回当前点赞状态和点赞总数
     * 
     * 幂等性：多次调用相同参数，结果一致
     * 
     * 扩展建议：可以在这里调用NotificationMessageService发送点赞通知给帖子作者
     * 
     * @param postId 帖子ID
     * @param userId 操作用户ID
     * @return 包含isLiked（是否已点赞）和likeCount（点赞总数）的响应
     */
    public ApiResponse<Map<String, Object>> togglePostLike(Long postId, Long userId) {
        Post post = postMapper.findById(postId);
        if (post == null) {
            return ApiResponse.error(404, "帖子不存在");
        }
        if (!"PUBLISHED".equals(post.getStatus())) {
            return ApiResponse.error(400, "帖子已下架，无法点赞");
        }

        Reaction existing = reactionMapper.findByUserIdAndPostId(userId, postId, "LIKE");
        boolean isLiked;
        
        if (existing != null) {
            // 取消点赞
            reactionMapper.deleteByUserIdAndPostId(userId, postId, "LIKE");
            isLiked = false;
        } else {
            // 点赞
            Reaction reaction = new Reaction();
            reaction.setPostId(postId);
            reaction.setUserId(userId);
            reaction.setType("LIKE");
            reactionMapper.insert(reaction);
            isLiked = true;
        }

        int likeCount = reactionMapper.countByPostId(postId);
        
        Map<String, Object> result = new HashMap<>();
        result.put("isLiked", isLiked);
        result.put("likeCount", likeCount);
        
        return ApiResponse.success(result);
    }

    /**
     * 点赞/取消点赞评论（幂等操作）
     * 
     * 功能说明：
     * - 验证评论存在且状态为VISIBLE
     * - 如果用户已点赞，则取消点赞（删除记录）
     * - 如果用户未点赞，则点赞（插入记录）
     * - 返回当前点赞状态和点赞总数
     * 
     * 幂等性：多次调用相同参数，结果一致
     * 
     * 扩展建议：可以在这里调用NotificationMessageService发送点赞通知给评论作者
     * 
     * @param commentId 评论ID
     * @param userId 操作用户ID
     * @return 包含isLiked（是否已点赞）和likeCount（点赞总数）的响应
     */
    public ApiResponse<Map<String, Object>> toggleCommentLike(Long commentId, Long userId) {
        Comment comment = commentMapper.findById(commentId);
        if (comment == null) {
            return ApiResponse.error(404, "评论不存在");
        }
        if (!"VISIBLE".equals(comment.getStatus())) {
            return ApiResponse.error(400, "评论已删除，无法点赞");
        }

        Reaction existing = reactionMapper.findByUserIdAndCommentId(userId, commentId, "LIKE");
        boolean isLiked;
        
        if (existing != null) {
            // 取消点赞
            reactionMapper.deleteByUserIdAndCommentId(userId, commentId, "LIKE");
            isLiked = false;
        } else {
            // 点赞
            Reaction reaction = new Reaction();
            reaction.setCommentId(commentId);
            reaction.setUserId(userId);
            reaction.setType("LIKE");
            reactionMapper.insert(reaction);
            isLiked = true;
        }

        int likeCount = reactionMapper.countByCommentId(commentId);
        
        Map<String, Object> result = new HashMap<>();
        result.put("isLiked", isLiked);
        result.put("likeCount", likeCount);
        
        return ApiResponse.success(result);
    }
}

