package com.adoption.community.model;

/**
 * 举报实体类
 *
 * 作用：表示用户对帖子或评论的举报信息
 *
 * 字段说明：
 * - id: 举报记录唯一标识
 * - postId: 被举报的帖子ID
 * - commentId: 被举报的评论ID
 * - reporterId: 举报人用户ID
 * - reason: 举报原因
 * - status: 处理状态（PENDING-待处理, REVIEWED-已处理）
 * - reviewedBy: 处理人用户ID（客服人员）
 * - reviewedAt: 处理时间
 * - createdAt: 创建时间
 *
 * 注意：
 * - postId和commentId只能有一个不为null，表示举报的是帖子还是评论
 * - 同一用户对同一帖子/评论只能举报一次，防止重复举报
 */
public class Report {
    /** 举报记录唯一标识 */
    private Long id;

    /**
     * 被举报的帖子ID
     * 举报帖子时使用，此时commentId应为null
     */
    private Long postId;

    /**
     * 被举报的评论ID
     * 举报评论时使用，此时postId应为null
     */
    private Long commentId;

    /** 举报人用户ID */
    private Long reporterId;

    /** 举报原因（用户填写的举报理由） */
    private String reason;

    /**
     * 处理状态
     * - PENDING: 待处理（等待客服审核）
     * - REVIEWED: 已处理（客服已审核）
     */
    private String status;

    /** 处理人用户ID（负责审核的客服人员） */
    private Long reviewedBy;

    /** 处理时间（格式：yyyy-MM-dd HH:mm:ss） */
    private String reviewedAt;

    /** 创建时间（格式：yyyy-MM-dd HH:mm:ss） */
    private String createdAt;

    // === 以下字段不存储在数据库，仅用于前端显示 ===

    /** 举报人姓名（非数据库字段，动态查询） */
    private String reporterName;

    /** 被举报的帖子标题（非数据库字段，动态查询） */
    private String postTitle;

    /** 被举报的评论内容（非数据库字段，动态查询） */
    private String commentContent;

    /** 被举报内容的作者姓名（非数据库字段，动态查询） */
    private String targetAuthorName;

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

    public Long getReporterId() {
        return reporterId;
    }

    public void setReporterId(Long reporterId) {
        this.reporterId = reporterId;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public Long getReviewedBy() {
        return reviewedBy;
    }

    public void setReviewedBy(Long reviewedBy) {
        this.reviewedBy = reviewedBy;
    }

    public String getReviewedAt() {
        return reviewedAt;
    }

    public void setReviewedAt(String reviewedAt) {
        this.reviewedAt = reviewedAt;
    }

    public String getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(String createdAt) {
        this.createdAt = createdAt;
    }

    public String getReporterName() {
        return reporterName;
    }

    public void setReporterName(String reporterName) {
        this.reporterName = reporterName;
    }

    public String getPostTitle() {
        return postTitle;
    }

    public void setPostTitle(String postTitle) {
        this.postTitle = postTitle;
    }

    public String getCommentContent() {
        return commentContent;
    }

    public void setCommentContent(String commentContent) {
        this.commentContent = commentContent;
    }

    public String getTargetAuthorName() {
        return targetAuthorName;
    }

    public void setTargetAuthorName(String targetAuthorName) {
        this.targetAuthorName = targetAuthorName;
    }
}

