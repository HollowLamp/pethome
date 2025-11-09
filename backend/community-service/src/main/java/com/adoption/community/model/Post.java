package com.adoption.community.model;

/**
 * 帖子实体类
 *
 * 作用：表示社区中的帖子信息，包括宠物发布、日常分享、养宠指南等类型
 *
 * 字段说明：
 * - id: 帖子唯一标识
 * - authorId: 发布者用户ID
 * - type: 帖子类型（PET_PUBLISH-宠物发布, DAILY-日常分享, GUIDE-养宠指南）
 * - title: 帖子标题
 * - content: 帖子正文内容
 * - mediaUrls: 媒体文件URL列表（JSON字符串格式，存储图片或视频的URL数组）
 * - bindPetId: 绑定的宠物ID（仅宠物发布类型帖子使用）
 * - aiSummary: AI生成的帖子摘要
 * - aiFlagged: AI标记是否违规（true-标记为违规，false-正常）
 * - status: 帖子状态（PUBLISHED-已发布, FLAGGED-标记违规, REMOVED-已删除）
 * - recommend: 是否推荐（true-推荐到首页, false-不推荐）
 * - createdAt: 创建时间
 * - updatedAt: 更新时间
 */
public class Post {
    /** 帖子唯一标识 */
    private Long id;

    /** 发布者用户ID */
    private Long authorId;

    /**
     * 帖子类型
     * - PET_PUBLISH: 宠物发布（发布待领养的宠物信息）
     * - DAILY: 日常分享（用户分享养宠日常）
     * - GUIDE: 养宠指南（分享养宠知识和经验）
     */
    private String type;

    /** 帖子标题 */
    private String title;

    /** 帖子正文内容（支持Markdown格式） */
    private String content;

    /**
     * 媒体文件URL列表（JSON字符串格式）
     * 存储格式示例：["http://example.com/image1.jpg", "http://example.com/video1.mp4"]
     * 支持图片和视频文件，通过文件上传接口获取URL后存储
     */
    private String mediaUrls;

    /**
     * 绑定的宠物ID
     * 仅当type为PET_PUBLISH时使用，关联待领养的宠物信息
     */
    private Long bindPetId;

    /** AI生成的帖子摘要（用于快速预览） */
    private String aiSummary;

    /**
     * AI标记是否违规
     * true: 被AI标记为可能违规，需要人工审核
     * false: 正常内容
     */
    private Boolean aiFlagged;

    /**
     * 帖子状态
     * - PUBLISHED: 已发布（正常显示）
     * - FLAGGED: 标记违规（需要审核）
     * - REMOVED: 已删除（不显示）
     */
    private String status;

    /**
     * 是否推荐
     * true: 推荐到首页，优先展示
     * false: 普通帖子，按时间或热度排序
     */
    private Boolean recommend;

    /** 创建时间（格式：yyyy-MM-dd HH:mm:ss） */
    private String createdAt;

    /** 更新时间（格式：yyyy-MM-dd HH:mm:ss） */
    private String updatedAt;

    // === 以下字段不存储在数据库，仅用于前端显示 ===

    /** 点赞数（非数据库字段，动态统计） */
    private Integer likeCount;

    /** 评论数（非数据库字段，动态统计） */
    private Integer commentCount;

    /** 当前用户是否已点赞（非数据库字段，动态查询） */
    private Boolean isLiked;

    /** 帖子作者用户名（非数据库字段，动态查询） */
    private String authorName;

    /** 帖子作者头像URL（非数据库字段，动态查询） */
    private String authorAvatarUrl;

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getAuthorId() {
        return authorId;
    }

    public void setAuthorId(Long authorId) {
        this.authorId = authorId;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public String getMediaUrls() {
        return mediaUrls;
    }

    public void setMediaUrls(String mediaUrls) {
        this.mediaUrls = mediaUrls;
    }

    public Long getBindPetId() {
        return bindPetId;
    }

    public void setBindPetId(Long bindPetId) {
        this.bindPetId = bindPetId;
    }

    public String getAiSummary() {
        return aiSummary;
    }

    public void setAiSummary(String aiSummary) {
        this.aiSummary = aiSummary;
    }

    public Boolean getAiFlagged() {
        return aiFlagged;
    }

    public void setAiFlagged(Boolean aiFlagged) {
        this.aiFlagged = aiFlagged;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public Boolean getRecommend() {
        return recommend;
    }

    public void setRecommend(Boolean recommend) {
        this.recommend = recommend;
    }

    public String getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(String createdAt) {
        this.createdAt = createdAt;
    }

    public String getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(String updatedAt) {
        this.updatedAt = updatedAt;
    }

    public Integer getLikeCount() {
        return likeCount;
    }

    public void setLikeCount(Integer likeCount) {
        this.likeCount = likeCount;
    }

    public Integer getCommentCount() {
        return commentCount;
    }

    public void setCommentCount(Integer commentCount) {
        this.commentCount = commentCount;
    }

    public Boolean getIsLiked() {
        return isLiked;
    }

    public void setIsLiked(Boolean isLiked) {
        this.isLiked = isLiked;
    }

    public String getAuthorName() {
        return authorName;
    }

    public void setAuthorName(String authorName) {
        this.authorName = authorName;
    }

    public String getAuthorAvatarUrl() {
        return authorAvatarUrl;
    }

    public void setAuthorAvatarUrl(String authorAvatarUrl) {
        this.authorAvatarUrl = authorAvatarUrl;
    }
}

