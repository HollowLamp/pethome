package com.adoption.community.model;

/**
 * 互动反应实体类
 * 
 * 作用：表示用户对帖子或评论的互动行为（如点赞）
 * 
 * 字段说明：
 * - id: 反应记录唯一标识
 * - postId: 帖子ID（对帖子点赞时使用）
 * - commentId: 评论ID（对评论点赞时使用）
 * - userId: 操作用户ID
 * - type: 反应类型（目前仅支持LIKE-点赞）
 * - createdAt: 创建时间
 * 
 * 注意：
 * - postId和commentId只能有一个不为null，表示是对帖子还是评论的互动
 * - 同一用户对同一帖子/评论只能有一条记录，实现点赞/取消点赞的幂等性
 */
public class Reaction {
    /** 反应记录唯一标识 */
    private Long id;
    
    /** 
     * 帖子ID
     * 对帖子点赞时使用，此时commentId应为null
     */
    private Long postId;
    
    /** 
     * 评论ID
     * 对评论点赞时使用，此时postId应为null
     */
    private Long commentId;
    
    /** 操作用户ID */
    private Long userId;
    
    /** 
     * 反应类型
     * 目前仅支持：LIKE（点赞）
     * 未来可扩展：DISLIKE（点踩）、BOOKMARK（收藏）等
     */
    private String type;
    
    /** 创建时间（格式：yyyy-MM-dd HH:mm:ss） */
    private String createdAt;

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getPostId() {
        return postId;
    }

    public void setPostId(Long postId) {
        this.postId = postId;
    }

    public Long getCommentId() {
        return commentId;
    }

    public void setCommentId(Long commentId) {
        this.commentId = commentId;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(String createdAt) {
        this.createdAt = createdAt;
    }
}

