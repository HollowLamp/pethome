package com.adoption.org.entity;

import java.time.LocalDateTime;

/**
 * 对应表 org_member
 * 每条记录代表某一个用户属于某一个机构
 *
 * 设计说明：
 * - 不持久化角色列；
 * - 是否机构拥有者通过对比 Organization.createdBy 与 userId 来推断；
 * - 是否机构成员通过是否存在该记录来判断。
 */
public class OrgMember {

    private Long id;                 // 主键
    private Long orgId;              // 所属机构ID
    private Long userId;             // 用户ID
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
    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
