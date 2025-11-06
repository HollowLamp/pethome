package com.adoption.org.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * 请求机构入驻申请时的参数结构体
 * 前端提交入驻申请时，将机构的基础信息通过这个 DTO 传给 org-service
 */
public class OrganizationApplyRequest {

    // 机构名称
    @NotBlank(message = "机构名称不能为空")
    private String name;

    // 资质证明文件URL，例如营业执照扫描件的存储路径
    @NotBlank(message = "资质证明URL不能为空")
    private String licenseUrl;

    // 机构地址
    @NotBlank(message = "机构地址不能为空")
    private String address;

    // 机构联系人名称
    @NotBlank(message = "联系人不能为空")
    private String contactName;

    // 联系人电话
    @NotBlank(message = "联系人电话不能为空")
    private String contactPhone;

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
}
