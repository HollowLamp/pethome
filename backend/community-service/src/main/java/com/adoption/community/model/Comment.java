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

    /** 点赞数（非数据库字段，动态查询） */
    private Integer likeCount;

    /** 当前用户是否已点赞（非数据库字段，动态查询） */
    private Boolean isLiked;

    /** 评论作者用户ID（用于前端显示，等同于authorId） */
    private Long userId;

    /** 评论作者用户名（非数据库字段，动态查询） */
    private String userName;

    /** 评论作者头像URL（非数据库字段，动态查询） */
    private String userAvatarUrl;

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

    public Integer getLikeCount() {
        return likeCount;
    }

    public void setLikeCount(Integer likeCount) {
        this.likeCount = likeCount;
    }

    public Boolean getIsLiked() {
        return isLiked;
    }

    public void setIsLiked(Boolean isLiked) {
        this.isLiked = isLiked;
    }

    public Long getUserId() {
        return userId != null ? userId : authorId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public String getUserName() {
        return userName;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }

    public String getUserAvatarUrl() {
        return userAvatarUrl;
    }

    public void setUserAvatarUrl(String userAvatarUrl) {
        this.userAvatarUrl = userAvatarUrl;
    }
}

