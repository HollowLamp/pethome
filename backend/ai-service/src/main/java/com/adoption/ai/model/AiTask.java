package com.adoption.ai.model;

import java.time.LocalDateTime;

/**
 * AI 任务实体类
 */
public class AiTask {
    private Long id;
    private String type; // STATE_EXTRACT, CONTENT_MOD, SUMMARY
    private Long sourceId;
    private String sourceType; // POST, PET
    private String status; // PENDING, DONE, FAILED
    private String resultJson; // JSON 格式的结果
    private java.math.BigDecimal confidence; // 置信度 0.00-1.00
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public Long getSourceId() {
        return sourceId;
    }

    public void setSourceId(Long sourceId) {
        this.sourceId = sourceId;
    }

    public String getSourceType() {
        return sourceType;
    }

    public void setSourceType(String sourceType) {
        this.sourceType = sourceType;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getResultJson() {
        return resultJson;
    }

    public void setResultJson(String resultJson) {
        this.resultJson = resultJson;
    }

    public java.math.BigDecimal getConfidence() {
        return confidence;
    }

    public void setConfidence(java.math.BigDecimal confidence) {
        this.confidence = confidence;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
}

