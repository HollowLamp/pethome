package com.adoption.adoption.service;

import com.adoption.adoption.model.AdoptionApp;
import com.adoption.adoption.model.InterviewRecord;
import com.adoption.adoption.repository.AdoptionAppMapper;
import com.adoption.adoption.repository.AdoptionDocMapper;
import com.adoption.adoption.repository.InterviewRecordMapper;
import com.adoption.common.api.ApiResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class AdoptionService {

    // 注入Mapper层Bean对象
    @Autowired
    private AdoptionAppMapper adoptionAppMapper;
    @Autowired
    private AdoptionDocMapper adoptionDocMapper;
    @Autowired
    private InterviewRecordMapper interviewRecordMapper;

    // 用户 - 提交领养申请
    public AdoptionApp submitAdoption(AdoptionApp adoptionApp) {
        adoptionApp.setStatus("PENDING");
        adoptionApp.setCreatedAt(LocalDateTime.now());
        adoptionApp.setUpdatedAt(LocalDateTime.now());
        adoptionAppMapper.insert(adoptionApp);
        return adoptionApp;
    }

    // 用户 - 查看我的申请
    public List<AdoptionApp> getMyApplications(Long applicantId) {
        return adoptionAppMapper.selectByApplicantId(applicantId);
    }

    // 用户 - 查看申请详情
    public AdoptionApp getApplicationDetail(Long id) {
        return adoptionAppMapper.selectById(id);
    }

    // 用户 - 查看已领养宠物
    public List<AdoptionApp> getAdoptedPets(Long applicantId) {
        return adoptionAppMapper.selectAdoptedPets(applicantId);
    }

    // 机构管理员 - 查看待审核申请
    public List<AdoptionApp> getPendingApplications(Long orgId, String status) {
        if (status == null || status.isEmpty()) {
            status = "PENDING";
        }
        return adoptionAppMapper.selectByOrgIdAndStatus(orgId, status);
    }

    // 机构管理员 - 初审申请通过
    public boolean approveApplication(Long id, Long orgId) {
        AdoptionApp adoptionApp = adoptionAppMapper.selectById(id);
        if (adoptionApp != null && adoptionApp.getOrgId().equals(orgId)) {
            adoptionApp.setStatus("ORG_APPROVED");
            adoptionApp.setUpdatedAt(LocalDateTime.now());
            return adoptionAppMapper.updateStatus(adoptionApp) > 0;
        }
        return false;
    }

    // 机构管理员 - 初审申请拒绝
    public boolean rejectApplication(Long id, Long orgId, String rejectReason) {
        AdoptionApp adoptionApp = adoptionAppMapper.selectById(id);
        if (adoptionApp != null && adoptionApp.getOrgId().equals(orgId)) {
            adoptionApp.setStatus("ORG_REJECTED");
            adoptionApp.setRejectReason(rejectReason);
            adoptionApp.setUpdatedAt(LocalDateTime.now());
            return adoptionAppMapper.updateStatus(adoptionApp) > 0;
        }
        return false;
    }

    // 审核员 - 复审申请 (批准)
    public boolean platformApproveApplication(Long id) {
        AdoptionApp adoptionApp = adoptionAppMapper.selectById(id);
        System.out.println(adoptionApp.getStatus());
        if (adoptionApp != null && adoptionApp.getStatus().equals("ORG_APPROVED")) { // 前提是机构管理员审核通过
            adoptionApp.setStatus("PLATFORM_APPROVED");
            adoptionApp.setUpdatedAt(LocalDateTime.now());
            return adoptionAppMapper.updateStatus(adoptionApp) > 0;
        }
        return false;
    }

    // 审核员 - 复审申请 (拒绝)
    public boolean platformRejectApplication(Long id, String rejectReason) {
        AdoptionApp adoptionApp = adoptionAppMapper.selectById(id);
        if (adoptionApp != null && adoptionApp.getStatus().equals("ORG_APPROVED")) { // 前提是机构管理员审核通过
            adoptionApp.setStatus("PLATFORM_REJECTED");
            adoptionApp.setRejectReason(rejectReason);
            adoptionApp.setUpdatedAt(LocalDateTime.now());
            return adoptionAppMapper.updateStatus(adoptionApp) > 0;
        }
        return false;
    }

    // 机构管理员 - 确认交接完成
    public boolean completeHandover(Long id, Long orgId) {
        AdoptionApp adoptionApp = adoptionAppMapper.selectById(id);
        if (adoptionApp != null && adoptionApp.getOrgId().equals(orgId)) {
            adoptionApp.setStatus("COMPLETED");
            adoptionApp.setUpdatedAt(LocalDateTime.now());
            return adoptionAppMapper.updateStatus(adoptionApp) > 0;
        }
        return false;
    }
}