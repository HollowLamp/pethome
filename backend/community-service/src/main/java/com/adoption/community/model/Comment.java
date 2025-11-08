package com.adoption.community.model;

/**
 * 评论实体类
 * 
 * 作用：表示用户对帖子的评论信息
 * 
 * 字段说明：
 * - id: 评论唯一标识
 * - postId: 所属帖子ID
 * - authorId: 评论作者用户ID
 * - content: 评论内容
 * - status: 评论状态（VISIBLE-可见, REMOVED-已删除）
 * - createdAt: 创建时间
 */
public class Comment {
    /** 评论唯一标识 */
    private Long id;
    
    /** 所属帖子ID */
    private Long postId;
    
    /** 评论作者用户ID */
    private Long authorId;
    
    /** 评论内容（支持文本，不支持图片） */
    private String content;
    
    /** 
     * 评论状态
     * - VISIBLE: 可见（正常显示）
     * - REMOVED: 已删除（不显示，但保留记录）
     */
    private String status;
    
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

    public Long getAuthorId() {
        return authorId;
    }

    public void setAuthorId(Long authorId) {
        this.authorId = authorId;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(String createdAt) {
        this.createdAt = createdAt;
    }
}

