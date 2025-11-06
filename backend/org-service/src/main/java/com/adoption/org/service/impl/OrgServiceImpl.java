package com.adoption.org.service.impl;

import com.adoption.common.api.ApiResponse;
import com.adoption.common.constant.RoleEnum;
import com.adoption.org.dto.OrganizationApplyRequest;
import com.adoption.org.dto.OrganizationApproveRequest;
import com.adoption.org.dto.AddMemberRequest;
import com.adoption.org.entity.Organization;
import com.adoption.org.entity.OrgStatus;
import com.adoption.org.entity.OrgMember;
import com.adoption.org.repository.OrganizationMapper;
import com.adoption.org.repository.OrgMemberMapper;
import com.adoption.org.service.OrgService;
import com.adoption.org.event.OrgEvent;
import com.adoption.org.event.OrgEventPublisher;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;


/**
 * OrgServiceImpl
 * 机构服务业务逻辑实现类
 */
@Service
public class OrgServiceImpl implements OrgService {

    private final OrganizationMapper organizationMapper;
    private final OrgMemberMapper orgMemberMapper;

    private final OrgEventPublisher eventPublisher;

    // 构造注入 mapper
    public OrgServiceImpl(OrganizationMapper organizationMapper,
                          OrgMemberMapper orgMemberMapper,
                          OrgEventPublisher eventPublisher) {
        this.organizationMapper = organizationMapper;
        this.orgMemberMapper = orgMemberMapper;
        this.eventPublisher = eventPublisher; // 注入事件发布器
    }

    @Override
    public ApiResponse<String> apply(OrganizationApplyRequest request, String userId) {
        // 1）构造Organization实体对象
        Organization org = new Organization();
        org.setName(request.getName());
        org.setLicenseUrl(request.getLicenseUrl());
        org.setAddress(request.getAddress());
        org.setContactName(request.getContactName());
        org.setContactPhone(request.getContactPhone());

        // 2）初始状态：待审核
        org.setStatus(OrgStatus.PENDING);

        // 3）设置创建人（来自Gateway透传的 X-User-Id）
        org.setCreatedBy(Long.valueOf(userId));

        // 4）设置时间戳（后面我们约定由代码维护）
        LocalDateTime now = LocalDateTime.now();
        org.setCreatedAt(now);
        org.setUpdatedAt(now);

        // 5）落库
        organizationMapper.insert(org);

        // 6）发布事件 event.org.applied
        Long orgId = org.getId(); // 取到新机构ID（依赖 @Options(useGeneratedKeys=true)）
        eventPublisher.publish(OrgEvent.ORG_APPLIED, orgId); // 发布 "提交入驻" 事件

        // 7）返回成功
        return ApiResponse.success("提交成功，等待审核");
    }


    @Override
    public ApiResponse<String> approve(Long orgId, OrganizationApproveRequest request) {

        // 1. 查询机构是否存在
        Organization org = organizationMapper.findById(orgId);
        if (org == null) {
            return ApiResponse.error(404, "机构不存在");
        }

        // 2. 仅允许 PENDING 状态机构通过审核
        if (org.getStatus() != OrgStatus.PENDING) {
            return ApiResponse.error(400, "当前状态不允许审核通过");
        }

        // 3. 修改状态为 APPROVED
        LocalDateTime now = LocalDateTime.now();
        organizationMapper.updateStatus(orgId, OrgStatus.APPROVED.name(), now);

        // 4. 审核通过后，将 created_by 用户加入 org_member，角色为 ORG_ADMIN（防止重复插入）
        OrgMember existing = orgMemberMapper.findByOrgIdAndUserId(orgId, org.getCreatedBy());
        if (existing == null) {
            OrgMember owner = new OrgMember();
            owner.setOrgId(orgId);
            owner.setUserId(org.getCreatedBy());
            owner.setRole(RoleEnum.ORG_ADMIN);
            owner.setCreatedAt(now);
            orgMemberMapper.insert(owner);
        }

        // 5. 发布 event.org.approved
        eventPublisher.publish(OrgEvent.ORG_APPROVED, orgId);

        return ApiResponse.success("审核通过，机构创建者已成为 ORG_ADMIN");
    }



    @Override
    public ApiResponse<String> reject(Long orgId, OrganizationApproveRequest request) {

        // 1）先查 org
        Organization org = organizationMapper.findById(orgId);
        if (org == null) {
            return ApiResponse.error(404, "机构不存在");
        }

        // 2）只能从 PENDING → REJECTED
        if (org.getStatus() != OrgStatus.PENDING) {
            return ApiResponse.error(400, "当前状态不允许拒绝");
        }

        // 3）修改状态
        LocalDateTime now = LocalDateTime.now();
        organizationMapper.updateStatus(orgId, OrgStatus.REJECTED.name(), now);

        // 4）如果企业需要记录拒绝理由，可以在未来扩展审核记录表
        //    现在只是基础版本先完成流程
        //    request.getReason() 未来会进入 org_review_log

        // 5）event.org.rejected
        eventPublisher.publish(OrgEvent.ORG_REJECTED, orgId);

        return ApiResponse.success("审核已拒绝");
    }


    @Override
    public ApiResponse<Object> getDetail(Long orgId) {

        Organization org = organizationMapper.findById(orgId);
        if (org == null) {
            return ApiResponse.error(404, "机构不存在");
        }

        // 基本版本先不拼成员/统计等
        // 后续可扩展为带成员/审核记录等详情
        return ApiResponse.success(org);
    }


    @Override
    public ApiResponse<String> addMember(Long orgId, AddMemberRequest request) {

        // 1. 检查机构是否存在
        Organization org = organizationMapper.findById(orgId);
        if (org == null) {
            return ApiResponse.error(404, "机构不存在");
        }

        // 2. 检查成员是否已存在
        OrgMember existing = orgMemberMapper.findByOrgIdAndUserId(orgId, request.getUserId());
        if (existing != null) {
            return ApiResponse.error(400, "该用户已是机构成员");
        }

        // 3. 构造新成员记录（仅允许 ORG_ADMIN / ORG_STAFF）
        RoleEnum role;
        try {
            role = RoleEnum.valueOf(request.getRole().trim().toUpperCase());
        } catch (Exception ex) {
            return ApiResponse.error(400, "非法角色");
        }
        if (role != RoleEnum.ORG_ADMIN && role != RoleEnum.ORG_STAFF) {
            return ApiResponse.error(400, "不支持的机构角色");
        }

        OrgMember member = new OrgMember();
        member.setOrgId(orgId);
        member.setUserId(request.getUserId());
        member.setRole(role);   // 仅接受 ORG_ADMIN / ORG_STAFF
        member.setCreatedAt(LocalDateTime.now());

        // 4. 插入数据库
        orgMemberMapper.insert(member);

        return ApiResponse.success("成员添加成功");
    }


    @Override
    public ApiResponse<String> deleteMember(Long orgId, Long userId) {

        // 1. 检查成员是否存在
        OrgMember existing = orgMemberMapper.findByOrgIdAndUserId(orgId, userId);
        if (existing == null) {
            return ApiResponse.error(404, "该成员不存在");
        }

        // 2. 执行删除
        orgMemberMapper.deleteByOrgIdAndUserId(orgId, userId);

        return ApiResponse.success("成员删除成功");
    }


    @Override
    public ApiResponse<Object> getMembers(Long orgId) {

        // 查询机构成员列表
        List<OrgMember> members = orgMemberMapper.findByOrgId(orgId);

        return ApiResponse.success(members);
    }

}
