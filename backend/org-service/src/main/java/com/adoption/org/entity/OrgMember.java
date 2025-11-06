package com.adoption.org.entity;

import com.adoption.common.constant.RoleEnum;
import java.time.LocalDateTime;

/**
 * 对应表 org_member
 * 每条记录代表某一个用户属于某一个机构，并在机构内拥有一定角色
 */
public class OrgMember {

    private Long id;                 // 主键
    private Long orgId;              // 所属机构ID
    private Long userId;             // 用户ID
    private RoleEnum role;           // 成员角色（仅 ORG_ADMIN / ORG_STAFF 用于 org-service）
    private LocalDateTime createdAt; // 加入时间

    // Getter / Setter
    public Long getId() {
        return id;
    }
    public void setId(Long id) {
        this.id = id;
    }
    public Long getOrgId() {
        return orgId;
    }
    public void setOrgId(Long orgId) {
        this.orgId = orgId;
    }
    public Long getUserId() {
        return userId;
    }
    public void setUserId(Long userId) {
        this.userId = userId;
    }
    public RoleEnum getRole() {
        return role;
    }
    public void setRole(RoleEnum role) {
        this.role = role;
    }
    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
