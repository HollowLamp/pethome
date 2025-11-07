package com.adoption.org.service;

import com.adoption.common.api.ApiResponse;
import com.adoption.org.dto.OrganizationApplyRequest;
import com.adoption.org.dto.OrganizationApproveRequest;
import com.adoption.org.dto.AddMemberRequest;

/**
 * OrgService
 * 机构服务业务接口定义
 *
 * 设计说明：
 * - Controller 只做参数接收与返回，所有业务处理集中在 Service
 * - 返回统一使用 ApiResponse<T>
 * - 权限判断建议在实现层结合 org/ org_member 数据完成
 */
public interface OrgService {

    // 机构提交入驻申请
    ApiResponse<String> apply(OrganizationApplyRequest request, String userId);

    // 管理员审核通过
    ApiResponse<String> approve(Long orgId, OrganizationApproveRequest request);

    // 管理员驳回申请
    ApiResponse<String> reject(Long orgId, OrganizationApproveRequest request);

    // 查询机构详情
    ApiResponse<Object> getDetail(Long orgId);

    // 添加成员
    ApiResponse<String> addMember(Long orgId, AddMemberRequest request);

    // 删除成员
    ApiResponse<String> deleteMember(Long orgId, Long userId);

    // 查询成员列表
    ApiResponse<Object> getMembers(Long orgId);

    // 查询用户所属机构列表
    ApiResponse<Object> getMemberships(Long userId);

    // 更新机构资质链接
    ApiResponse<com.adoption.org.entity.Organization> updateLicense(Long orgId, Long operatorId, String licenseUrl);

    // 获取待审核机构列表
    ApiResponse<java.util.List<com.adoption.org.entity.Organization>> listPendingOrganizations();
}
