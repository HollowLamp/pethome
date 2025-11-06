package com.adoption.org.dto;

import jakarta.validation.constraints.Size;

/**
 * 用于机构申请审批（通过或拒绝）时的请求参数体
 * reason 字段可作为备注，例如驳回理由或审核备注
 */
public class OrganizationApproveRequest {

    // 审批备注（可为空）
    @Size(max = 200, message = "备注最长200字符")
    private String reason;

    public String getReason() {
        return reason;
    }
    public void setReason(String reason) {
        this.reason = reason;
    }
}
