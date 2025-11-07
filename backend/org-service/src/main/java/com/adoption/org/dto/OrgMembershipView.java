package com.adoption.org.dto;

import com.adoption.org.entity.OrgStatus;

import java.time.LocalDateTime;

/**
 * 视图对象：用户在机构中的成员关系（org_member） + 机构信息（org）
 * 返回两张表的全部关键字段，便于前端直接使用。
 */
public class OrgMembershipView {

    // org_member 表字段
    private Long membershipId;
    private Long orgId;
    private Long userId;
    private LocalDateTime membershipCreatedAt;

    // org 表字段
    private Long organizationId;
    private String name;
    private String licenseUrl;
    private String address;
    private String contactName;
    private String contactPhone;
    private OrgStatus status;
    private Long createdBy;
    private LocalDateTime orgCreatedAt;
    private LocalDateTime orgUpdatedAt;

    public Long getMembershipId() {
        return membershipId;
    }
    public void setMembershipId(Long membershipId) {
        this.membershipId = membershipId;
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
    public LocalDateTime getMembershipCreatedAt() {
        return membershipCreatedAt;
    }
    public void setMembershipCreatedAt(LocalDateTime membershipCreatedAt) {
        this.membershipCreatedAt = membershipCreatedAt;
    }
    public Long getOrganizationId() {
        return organizationId;
    }
    public void setOrganizationId(Long organizationId) {
        this.organizationId = organizationId;
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
    public LocalDateTime getOrgCreatedAt() {
        return orgCreatedAt;
    }
    public void setOrgCreatedAt(LocalDateTime orgCreatedAt) {
        this.orgCreatedAt = orgCreatedAt;
    }
    public LocalDateTime getOrgUpdatedAt() {
        return orgUpdatedAt;
    }
    public void setOrgUpdatedAt(LocalDateTime orgUpdatedAt) {
        this.orgUpdatedAt = orgUpdatedAt;
    }
}


