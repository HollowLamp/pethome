package com.adoption.org.entity;

/**
 * 机构入驻流程状态枚举
 * 对应 org.status 字段
 * 存储方式为 STRING，例如: "PENDING", "APPROVED", "REJECTED"
 */
public enum OrgStatus {
    PENDING,    // 已提交，待审核
    APPROVED,   // 审核通过
    REJECTED    // 审核拒绝
}
