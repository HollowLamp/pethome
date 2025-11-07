package com.adoption.org.entity;

import java.time.LocalDateTime;

/**
 * 对应表 org
 * 机构基础信息实体类
 * 每条记录代表一个机构
 *
 * 字段说明：
 * - status：使用数据库 ENUM(PENDING/APPROVED/REJECTED)，在应用层对应 OrgStatus 枚举
 * - createdBy：创建人（入驻申请发起人），用于推断机构拥有者
 */
public class Organization {

    private Long id;                 // 主键 BIGINT
    private String name;             // 机构名称
    private String licenseUrl;       // 资质证明文件URL
    private String address;          // 地址
    private String contactName;      // 联系人姓名
    private String contactPhone;     // 联系电话
    private OrgStatus status;        // 入驻审核状态(PENDING/APPROVED/REJECTED)
    private Long createdBy;          // 创建人用户ID（申请机构的用户）
    private LocalDateTime createdAt; // 创建时间
    private LocalDateTime updatedAt; // 最近更新时间

    // Getter / Setter
    public Long getId() {
        return id;
    }
    public void setId(Long id) {
        this.id = id;
    }
    public String getName() {
        return name;
    }
    public void setName(String name) {
        this.name = name;
    }
    public String getLicenseUrl() {
        return licenseUrl;
    }
    public void setLicenseUrl(String licenseUrl) {
        this.licenseUrl = licenseUrl;
    }
    public String getAddress() {
        return address;
    }
    public void setAddress(String address) {
        this.address = address;
    }
    public String getContactName() {
        return contactName;
    }
    public void setContactName(String contactName) {
        this.contactName = contactName;
    }
    public String getContactPhone() {
        return contactPhone;
    }
    public void setContactPhone(String contactPhone) {
        this.contactPhone = contactPhone;
    }
    public OrgStatus getStatus() {
        return status;
    }
    public void setStatus(OrgStatus status) {
        this.status = status;
    }
    public Long getCreatedBy() {
        return createdBy;
    }
    public void setCreatedBy(Long createdBy) {
        this.createdBy = createdBy;
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
