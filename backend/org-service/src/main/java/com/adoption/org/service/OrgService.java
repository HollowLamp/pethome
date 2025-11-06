package com.adoption.org.service;

import com.adoption.common.api.ApiResponse;
import com.adoption.org.dto.OrganizationApplyRequest;
import com.adoption.org.dto.OrganizationApproveRequest;
import com.adoption.org.dto.AddMemberRequest;

/**
 * OrgService
 * 机构服务业务接口定义
 * Controller 不做逻辑运算，由此 Service 进行集中业务处理
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
}
