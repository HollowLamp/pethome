package com.adoption.adoption.service;

import com.adoption.adoption.feign.PetServiceClient;
import com.adoption.adoption.model.AdoptionApp;
import com.adoption.adoption.model.AdoptionDoc;
import com.adoption.adoption.model.InterviewRecord;
import com.adoption.adoption.repository.AdoptionAppMapper;
import com.adoption.adoption.repository.AdoptionDocMapper;
import com.adoption.adoption.repository.InterviewRecordMapper;
import com.adoption.common.api.ApiResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class AdoptionService {

    // 注入Mapper层Bean对象
    @Autowired
    private AdoptionAppMapper adoptionAppMapper;
    @Autowired
    private AdoptionDocMapper adoptionDocMapper;
    @Autowired
    private InterviewRecordMapper interviewRecordMapper;

    // 注入Feign客户端
    @Autowired
    private PetServiceClient petServiceClient;

    // 注入消息通知服务（用于发送 RabbitMQ 消息）
    @Autowired
    private NotificationMessageService notificationMessageService;

    // 用户 - 提交领养申请
    public AdoptionApp submitAdoption(AdoptionApp adoptionApp) {
        // 检查是否存在重复申请
        boolean exists = adoptionAppMapper.existsPendingApplication(
            adoptionApp.getApplicantId(),
            adoptionApp.getPetId()
        );
        if (exists) {
            throw new RuntimeException("您已对该宠物提交过申请，请勿重复申请");
        }

        adoptionApp.setStatus("PENDING");
        adoptionApp.setCreatedAt(LocalDateTime.now());
        adoptionApp.setUpdatedAt(LocalDateTime.now());
        adoptionAppMapper.insert(adoptionApp);

        // 发送 RabbitMQ 消息：通知申请人申请已提交
        // C端用例：注册用户提交领养申请 -> 收到系统通知（申请已提交）
        if (adoptionApp.getApplicantId() != null) {
            try {
                notificationMessageService.sendSystemNotification(
                        adoptionApp.getApplicantId(),
                        "领养申请已提交",
                        "您的领养申请已成功提交，等待机构审核",
                        "ADOPTION_SUBMITTED_TO_USER"
                );
            } catch (Exception e) {
                System.err.println("发送申请提交通知给用户失败: " + e.getMessage());
            }
        }

        // 发送 RabbitMQ 消息：通知机构管理员有新申请
        // 业务逻辑：用户提交申请后，需要通知该机构的机构管理员进行初审
        if (adoptionApp.getOrgId() != null) {
            try {
                notificationMessageService.sendSystemNotificationToOrgAdmin(
                        adoptionApp.getOrgId(),
                        "新的领养申请",
                        "收到新的领养申请，请及时进行初审审核",
                        "ADOPTION_SUBMITTED"
                );
            } catch (Exception e) {
                // 消息发送失败不影响主流程
                System.err.println("发送申请通知失败: " + e.getMessage());
            }
        }

        // 将用户的领养资料复制为申请材料
        Long appId = adoptionApp.getId();
        Long applicantId = adoptionApp.getApplicantId();
        if (appId != null && applicantId != null) {
            // 获取用户的领养资料
            List<AdoptionDoc> userDocs = adoptionDocMapper.selectUserProfileDocs(applicantId);
            // 将用户资料复制为申请材料
            for (AdoptionDoc userDoc : userDocs) {
                AdoptionDoc appDoc = new AdoptionDoc();
                appDoc.setAppId(appId); // 关联到申请ID
                // 处理 docType：去掉 USER_{userId}_ 前缀，只保留材料类型
                String docType = userDoc.getDocType();
                if (docType != null && docType.startsWith("USER_" + applicantId + "_")) {
                    appDoc.setDocType(docType.substring(("USER_" + applicantId + "_").length()));
                } else {
                    appDoc.setDocType(docType);
                }
                appDoc.setUrl(userDoc.getUrl());
                appDoc.setUploadedAt(LocalDateTime.now());
                adoptionDocMapper.insert(appDoc);
            }
        }

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

    // 获取申请材料列表（只返回申请材料，app_id 不为 NULL）
    public List<AdoptionDoc> getApplicationDocs(Long appId) {
        // 获取申请材料（app_id 不为 NULL，表示这是申请时提交的材料）
        List<AdoptionDoc> appDocs = adoptionDocMapper.selectByAppId(appId);
        return appDocs != null ? appDocs : new java.util.ArrayList<>();
    }

    // 用户 - 查看已领养宠物
    public List<AdoptionApp> getAdoptedPets(Long applicantId) {
        return adoptionAppMapper.selectAdoptedPets(applicantId);
    }

    // 检查用户是否领养了该宠物
    public boolean checkOwnership(Long petId, Long userId) {
        List<AdoptionApp> adoptedPets = adoptionAppMapper.selectAdoptedPets(userId);
        return adoptedPets.stream()
                .anyMatch(app -> app.getPetId().equals(petId) && "COMPLETED".equals(app.getStatus()));
    }

    // 获取已领养宠物列表（用于查询逾期未更新，供B端使用）
    public List<Map<String, Object>> getAdoptedPetsForReminder(Long orgId, Integer daysSinceUpdate) {
        // 查询所有已完成的领养申请
        List<AdoptionApp> allCompleted = new ArrayList<>();
        if (orgId != null) {
            // 如果指定了机构ID，查询该机构的所有已完成申请
            List<AdoptionApp> orgApps = adoptionAppMapper.selectByOrgIdAndStatus(orgId, "COMPLETED");
            allCompleted.addAll(orgApps);
        } else {
            // 查询所有已完成的申请（需要遍历所有用户，这里简化处理）
            // 实际项目中可以通过其他方式优化
            // 这里先返回空列表，后续可以优化
            return new ArrayList<>();
        }

        // 转换为Map格式，包含petId, applicantId, updatedAt等信息
        List<Map<String, Object>> result = new ArrayList<>();
        for (AdoptionApp app : allCompleted) {
            Map<String, Object> item = new HashMap<>();
            item.put("petId", app.getPetId());
            item.put("applicantId", app.getApplicantId());
            item.put("orgId", app.getOrgId());
            item.put("updatedAt", app.getUpdatedAt() != null ? app.getUpdatedAt().toString() : null);
            item.put("createdAt", app.getCreatedAt() != null ? app.getCreatedAt().toString() : null);
            result.add(item);
        }

        return result;
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
            boolean success = adoptionAppMapper.updateStatus(adoptionApp) > 0;

            // 更新宠物状态为 RESERVED（已预订）
            if (success && adoptionApp.getPetId() != null) {
                try {
                    Map<String, String> body = new HashMap<>();
                    body.put("status", "RESERVED");
                    petServiceClient.updatePetStatus(adoptionApp.getPetId(), body);
                } catch (Exception e) {
                    // 记录日志但不影响主流程
                    System.err.println("更新宠物状态失败，petId: " + adoptionApp.getPetId() + ", error: " + e.getMessage());
                }
            }

            // 发送 RabbitMQ 消息：通知申请人申请已通过初审
            // 当机构管理员审核通过时，通知申请人
            if (success && adoptionApp.getApplicantId() != null) {
                try {
                    notificationMessageService.sendSystemNotification(
                            adoptionApp.getApplicantId(),
                            "领养申请已通过初审",
                            "您的领养申请已通过机构初审，等待平台复审",
                            "ADOPTION_ORG_APPROVED"
                    );
                } catch (Exception e) {
                    // 消息发送失败不影响主流程
                    System.err.println("发送审核通过通知失败: " + e.getMessage());
                }
            }

            // 发送 RabbitMQ 消息：通知审核员（AUDITOR）准备复审
            // 业务逻辑：机构初审通过后，需要通知审核员进行平台复审
            if (success) {
                try {
                    notificationMessageService.sendSystemNotificationToRole(
                            "AUDITOR",
                            "待复审的领养申请",
                            "有领养申请已通过机构初审，等待平台复审",
                            "ADOPTION_PENDING_REVIEW"
                    );
                } catch (Exception e) {
                    System.err.println("发送复审通知失败: " + e.getMessage());
                }
            }

            return success;
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
            boolean success = adoptionAppMapper.updateStatus(adoptionApp) > 0;

            // 发送 RabbitMQ 消息：通知申请人申请已被拒绝
            if (success && adoptionApp.getApplicantId() != null) {
                try {
                    String body = "您的领养申请未通过机构初审";
                    if (rejectReason != null && !rejectReason.isEmpty()) {
                        body += "，原因：" + rejectReason;
                    }
                    notificationMessageService.sendSystemNotification(
                            adoptionApp.getApplicantId(),
                            "领养申请未通过初审",
                            body,
                            "ADOPTION_ORG_REJECTED"
                    );
                } catch (Exception e) {
                    System.err.println("发送拒绝通知失败: " + e.getMessage());
                }
            }

            return success;
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
            boolean success = adoptionAppMapper.updateStatus(adoptionApp) > 0;

            // 发送 RabbitMQ 消息：通知申请人申请已通过平台复审
            if (success && adoptionApp.getApplicantId() != null) {
                try {
                    notificationMessageService.sendSystemNotification(
                            adoptionApp.getApplicantId(),
                            "领养申请已通过平台审核",
                            "恭喜！您的领养申请已通过平台复审，可以安排面谈了",
                            "ADOPTION_PLATFORM_APPROVED"
                    );
                } catch (Exception e) {
                    System.err.println("发送平台审核通过通知失败: " + e.getMessage());
                }
            }

            // 发送 RabbitMQ 消息：通知机构管理员平台审核通过
            // 业务逻辑：平台审核通过后，通知机构管理员可以安排面谈了
            if (success && adoptionApp.getOrgId() != null) {
                try {
                    notificationMessageService.sendSystemNotificationToOrgAdmin(
                            adoptionApp.getOrgId(),
                            "领养申请已通过平台审核",
                            "领养申请已通过平台复审，可以安排面谈时间了",
                            "ADOPTION_PLATFORM_APPROVED_TO_ORG"
                    );
                } catch (Exception e) {
                    System.err.println("发送平台审核通过通知给机构失败: " + e.getMessage());
                }
            }

            return success;
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
            boolean success = adoptionAppMapper.updateStatus(adoptionApp) > 0;

            // 平台拒绝后，将宠物状态改回 AVAILABLE（可领养）
            if (success && adoptionApp.getPetId() != null) {
                try {
                    Map<String, String> body = new HashMap<>();
                    body.put("status", "AVAILABLE");
                    petServiceClient.updatePetStatus(adoptionApp.getPetId(), body);
                } catch (Exception e) {
                    // 记录日志但不影响主流程
                    System.err.println("更新宠物状态失败，petId: " + adoptionApp.getPetId() + ", error: " + e.getMessage());
                }
            }

            // 发送 RabbitMQ 消息：通知申请人申请已被平台拒绝
            if (success && adoptionApp.getApplicantId() != null) {
                try {
                    String body = "您的领养申请未通过平台复审";
                    if (rejectReason != null && !rejectReason.isEmpty()) {
                        body += "，原因：" + rejectReason;
                    }
                    notificationMessageService.sendSystemNotification(
                            adoptionApp.getApplicantId(),
                            "领养申请未通过平台审核",
                            body,
                            "ADOPTION_PLATFORM_REJECTED"
                    );
                } catch (Exception e) {
                    System.err.println("发送平台拒绝通知失败: " + e.getMessage());
                }
            }

            // 发送 RabbitMQ 消息：通知机构管理员平台审核拒绝
            // 业务逻辑：平台审核拒绝后，通知机构管理员，宠物状态已恢复为可领养
            if (success && adoptionApp.getOrgId() != null) {
                try {
                    notificationMessageService.sendSystemNotificationToOrgAdmin(
                            adoptionApp.getOrgId(),
                            "领养申请未通过平台审核",
                            "领养申请未通过平台复审，宠物状态已恢复为可领养",
                            "ADOPTION_PLATFORM_REJECTED_TO_ORG"
                    );
                } catch (Exception e) {
                    System.err.println("发送平台拒绝通知给机构失败: " + e.getMessage());
                }
            }

            return success;
        }
        return false;
    }

    // 机构管理员 - 确认交接完成
    public boolean completeHandover(Long id, Long orgId) {
        AdoptionApp adoptionApp = adoptionAppMapper.selectById(id);
        if (adoptionApp != null && adoptionApp.getOrgId().equals(orgId)) {
            adoptionApp.setStatus("COMPLETED");
            adoptionApp.setUpdatedAt(LocalDateTime.now());
            boolean success = adoptionAppMapper.updateStatus(adoptionApp) > 0;

            // 交接完成后，将宠物状态改为 ADOPTED（已领养）
            if (success && adoptionApp.getPetId() != null) {
                try {
                    Map<String, String> body = new HashMap<>();
                    body.put("status", "ADOPTED");
                    petServiceClient.updatePetStatus(adoptionApp.getPetId(), body);
                } catch (Exception e) {
                    // 记录日志但不影响主流程
                    System.err.println("更新宠物状态失败，petId: " + adoptionApp.getPetId() + ", error: " + e.getMessage());
                }
            }

            // 发送 RabbitMQ 消息：通知申请人交接已完成
            if (success && adoptionApp.getApplicantId() != null) {
                try {
                    notificationMessageService.sendSystemNotification(
                            adoptionApp.getApplicantId(),
                            "领养交接已完成",
                            "恭喜！您的领养交接已完成，宠物已成功领养",
                            "ADOPTION_COMPLETED"
                    );
                } catch (Exception e) {
                    System.err.println("发送交接完成通知失败: " + e.getMessage());
                }
            }

            // 发送 RabbitMQ 消息：通知机构管理员交接已完成
            // 业务逻辑：交接完成后，通知机构管理员领养流程已完成
            if (success && adoptionApp.getOrgId() != null) {
                try {
                    notificationMessageService.sendSystemNotificationToOrgAdmin(
                            adoptionApp.getOrgId(),
                            "领养交接已完成",
                            "领养交接已完成，宠物状态已更新为已领养",
                            "ADOPTION_COMPLETED_TO_ORG"
                    );
                } catch (Exception e) {
                    System.err.println("发送交接完成通知给机构失败: " + e.getMessage());
                }
            }

            // 发送 RabbitMQ 消息：通知宠物信息维护员（ORG_STAFF）交接已完成
            // 业务逻辑：交接完成后，通知宠物信息维护员可以开始跟踪已领养宠物的后续状态
            if (success && adoptionApp.getOrgId() != null) {
                try {
                    // 注意：这里发送给该机构的所有宠物信息维护员
                    // 可以通过 org-service 获取该机构的 ORG_STAFF 列表，暂时发送给所有 ORG_STAFF
                    notificationMessageService.sendSystemNotificationToRole(
                            "ORG_STAFF",
                            "新领养完成",
                            "有宠物已完成领养交接，请开始跟踪后续状态",
                            "ADOPTION_COMPLETED_TO_STAFF"
                    );
                } catch (Exception e) {
                    System.err.println("发送交接完成通知给维护员失败: " + e.getMessage());
                }
            }

            return success;
        }
        return false;
    }

    // 用户 - 上传领养资料材料
    public AdoptionDoc uploadUserProfileDoc(Long userId, String docType, String url) {
        AdoptionDoc doc = new AdoptionDoc();
        doc.setAppId(null); // app_id 为 NULL 表示用户资料
        doc.setDocType("USER_" + userId + "_" + docType); // 格式：USER_{userId}_{docType}
        doc.setUrl(url);
        doc.setUploadedAt(LocalDateTime.now());
        adoptionDocMapper.insert(doc);
        return doc;
    }

    // 用户 - 获取领养资料
    public List<AdoptionDoc> getUserProfileDocs(Long userId) {
        return adoptionDocMapper.selectUserProfileDocs(userId);
    }

    // 用户 - 删除领养资料中的某个材料
    public boolean deleteUserProfileDoc(Long docId) {
        return adoptionDocMapper.deleteUserProfileDoc(docId) > 0;
    }

    // 检查用户是否填写了领养资料
    public boolean hasUserProfile(Long userId) {
        return adoptionDocMapper.hasUserProfile(userId);
    }

    /**
     * 发送面谈确认通知给申请人
     *
     * 此方法供 interview-service 调用，当机构管理员确认面谈后，通知申请人面谈时间已确认
     * C端用例：注册用户收到系统通知 -> 面谈时间提醒
     *
     * @param appId 申请ID
     * @param interviewTime 面谈时间（格式：yyyy-MM-dd HH:mm）
     * @param interviewLocation 面谈地点（可选）
     */
    public void notifyInterviewConfirmed(Long appId, String interviewTime, String interviewLocation) {
        AdoptionApp adoptionApp = adoptionAppMapper.selectById(appId);
        if (adoptionApp != null && adoptionApp.getApplicantId() != null) {
            try {
                String body = "您的面谈时间已确认：" + interviewTime;
                if (interviewLocation != null && !interviewLocation.isEmpty()) {
                    body += "，地点：" + interviewLocation;
                }
                body += "，请准时参加面谈。";

                notificationMessageService.sendSystemNotification(
                        adoptionApp.getApplicantId(),
                        "面谈时间已确认",
                        body,
                        "INTERVIEW_CONFIRMED"
                );
            } catch (Exception e) {
                System.err.println("发送面谈确认通知失败: " + e.getMessage());
            }
        }
    }

    /**
     * 发送面谈预约请求通知给机构管理员
     *
     * 此方法供 interview-service 调用，当用户提交面谈预约请求后，通知机构管理员
     *
     * @param appId 申请ID
     */
    public void notifyInterviewRequested(Long appId) {
        AdoptionApp adoptionApp = adoptionAppMapper.selectById(appId);
        if (adoptionApp != null && adoptionApp.getOrgId() != null) {
            try {
                notificationMessageService.sendSystemNotificationToOrgAdmin(
                        adoptionApp.getOrgId(),
                        "新的面谈预约请求",
                        "有用户提交了面谈预约请求，请及时确认",
                        "INTERVIEW_REQUESTED"
                );
            } catch (Exception e) {
                System.err.println("发送面谈预约请求通知失败: " + e.getMessage());
            }
        }
    }
}