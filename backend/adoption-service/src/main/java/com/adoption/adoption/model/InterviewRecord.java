package com.adoption.adoption.model;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

// 面谈记录表 -- 对应数据库 interview_reward 表
@Getter
@Setter
public class InterviewRecord {
    private Long id;
    private Long appId;
    private Long orgId;
    private LocalDateTime startAt;
    private LocalDateTime endAt;
    private String status;
    private String note;

    // Constructors
    public InterviewRecord() {}

    public InterviewRecord(Long id, Long appId, Long orgId, LocalDateTime startAt, LocalDateTime endAt, String status, String note) {
        this.id = id;
        this.appId = appId;
        this.orgId = orgId;
        this.startAt = startAt;
        this.endAt = endAt;
        this.status = status;
        this.note = note;
    }
}