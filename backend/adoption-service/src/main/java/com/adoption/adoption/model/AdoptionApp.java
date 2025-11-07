package com.adoption.adoption.model;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

// 领养申请表（申请表核心信息） -- 对应数据库 adoption_app 表
@Getter
@Setter
public class AdoptionApp {
    private Long id;  // 主键
    private Long petId; // 宠物ID
    private Long applicantId; // 申请人ID
    private Long orgId; // 机构ID
    private String status;  // 申请状态：PENDING待审核, ORG_APPROVED机构管理员审核通过, ORG_REJECTED机构管理员审核不通过, PLATFORM_APPROVED平台管理员审核通过, PLATFORM_REJECTED平台管理员审核不通过, COMPLETED已完成
    private String rejectReason; // 驳回原因
    private LocalDateTime createdAt; // 创建时间
    private LocalDateTime updatedAt; // 更新时间

    // Constructors
    // 无参构造器
    public AdoptionApp() {}
    // 全参构造器
    public AdoptionApp(Long id, Long petId, Long applicantId, Long orgId, String status, String rejectReason, LocalDateTime createdAt, LocalDateTime updatedAt) {
        this.id = id;
        this.petId = petId;
        this.applicantId = applicantId;
        this.orgId = orgId;
        this.status = status;
        this.rejectReason = rejectReason;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }
}