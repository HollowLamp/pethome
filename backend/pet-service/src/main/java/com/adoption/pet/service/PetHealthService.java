package com.adoption.pet.service;

import com.adoption.common.api.ApiResponse;
import com.adoption.pet.feign.AdoptionServiceClient;
import com.adoption.pet.model.PetHealth;
import com.adoption.pet.repository.PetHealthMapper;
import com.adoption.pet.repository.PetMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class PetHealthService {
    private final PetHealthMapper petHealthMapper;
    private final PetMapper petMapper;
    private final AdoptionServiceClient adoptionServiceClient;
    private final NotificationMessageService notificationMessageService;

    public PetHealthService(PetHealthMapper petHealthMapper, PetMapper petMapper,
                           AdoptionServiceClient adoptionServiceClient,
                           NotificationMessageService notificationMessageService) {
        this.petHealthMapper = petHealthMapper;
        this.petMapper = petMapper;
        this.adoptionServiceClient = adoptionServiceClient;
        this.notificationMessageService = notificationMessageService;
    }

    /**
     * 更新健康/疫苗记录（维护员）
     * 每次更新都创建新记录，保留历史记录
     */
    public ApiResponse<PetHealth> updateHealth(Long petId, PetHealth health, Long updatedBy) {
        // 检查宠物是否存在
        if (petMapper.findById(petId) == null) {
            return ApiResponse.error(404, "宠物不存在");
        }

        health.setPetId(petId);
        health.setUpdatedBy(updatedBy);
        health.setUpdatedAt(LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));

        // 总是创建新记录，保留历史
        petHealthMapper.insert(health);

        // 返回刚创建的最新记录
        PetHealth updated = petHealthMapper.findLatestByPetId(petId);
        return ApiResponse.success(updated);
    }

    /**
     * 获取健康记录
     */
    public ApiResponse<PetHealth> getHealth(Long petId) {
        PetHealth health = petHealthMapper.findLatestByPetId(petId);
        if (health == null) {
            return ApiResponse.error(404, "暂无健康记录");
        }
        return ApiResponse.success(health);
    }

    /**
     * 获取所有健康记录历史
     */
    public ApiResponse<List<PetHealth>> getHealthHistory(Long petId) {
        List<PetHealth> history = petHealthMapper.findAllByPetId(petId);
        return ApiResponse.success(history);
    }

    /**
     * C端用户上传健康状态（需要验证用户是否领养了该宠物）
     */
    public ApiResponse<PetHealth> updateHealthByOwner(Long petId, PetHealth health, Long userId) {
        // 检查宠物是否存在
        if (petMapper.findById(petId) == null) {
            return ApiResponse.error(404, "宠物不存在");
        }

        // 验证用户是否领养了该宠物
        try {
            ApiResponse<Boolean> ownershipCheck = adoptionServiceClient.checkOwnership(petId, userId);
            if (ownershipCheck == null || ownershipCheck.getCode() != 200 || !Boolean.TRUE.equals(ownershipCheck.getData())) {
                return ApiResponse.error(403, "您未领养该宠物，无权更新健康状态");
            }
        } catch (Exception e) {
            return ApiResponse.error(500, "验证领养关系失败: " + e.getMessage());
        }

        health.setPetId(petId);
        health.setUpdatedBy(userId);
        health.setUpdatedAt(LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));

        // 总是创建新记录，保留历史
        petHealthMapper.insert(health);

        // 返回刚创建的最新记录
        PetHealth updated = petHealthMapper.findLatestByPetId(petId);
        return ApiResponse.success(updated);
    }

    /**
     * 查询逾期未更新健康状态的宠物（B端用）
     * @param orgId 机构ID（可选，如果提供则只查询该机构的宠物）
     * @param daysSinceUpdate 距离上次更新多少天算逾期（默认30天）
     * @return 逾期未更新的宠物列表，包含宠物信息、领养人信息、最后更新时间等
     */
    public ApiResponse<List<Map<String, Object>>> getOverduePets(Long orgId, Integer daysSinceUpdate) {
        if (daysSinceUpdate == null || daysSinceUpdate <= 0) {
            daysSinceUpdate = 30; // 默认30天
        }

        try {
            // 调用 adoption-service 获取已领养宠物列表
            ApiResponse<List<Map<String, Object>>> adoptedPetsResponse = adoptionServiceClient.getAdoptedPetsForReminder(orgId, daysSinceUpdate);
            if (adoptedPetsResponse == null || adoptedPetsResponse.getCode() != 200) {
                return ApiResponse.error(500, "获取已领养宠物列表失败");
            }

            List<Map<String, Object>> adoptedPets = adoptedPetsResponse.getData();
            if (adoptedPets == null || adoptedPets.isEmpty()) {
                return ApiResponse.success(new ArrayList<>());
            }

            List<Map<String, Object>> overduePets = new ArrayList<>();
            LocalDateTime now = LocalDateTime.now();

            for (Map<String, Object> adoptedPet : adoptedPets) {
                Object petIdObj = adoptedPet.get("petId");
                if (petIdObj == null) {
                    continue;
                }
                Long petId = Long.valueOf(petIdObj.toString());

                // 获取该宠物的最新健康记录
                PetHealth latestHealth = petHealthMapper.findLatestByPetId(petId);

                boolean isOverdue = false;
                String lastUpdateTime = null;
                long daysOverdue = 0;

                if (latestHealth == null || latestHealth.getUpdatedAt() == null) {
                    // 从未更新过健康状态，检查领养时间
                    Object completedAtObj = adoptedPet.get("updatedAt"); // 领养完成时间
                    if (completedAtObj != null) {
                        try {
                            String completedAtStr = completedAtObj.toString();
                            LocalDateTime completedAt = null;
                            try {
                                // 格式1: "yyyy-MM-dd HH:mm:ss"
                                DateTimeFormatter formatter1 = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
                                completedAt = LocalDateTime.parse(completedAtStr, formatter1);
                            } catch (Exception e1) {
                                try {
                                    // 格式2: "yyyy-MM-ddTHH:mm:ss" (ISO格式)
                                    completedAt = LocalDateTime.parse(completedAtStr.replace(" ", "T"));
                                } catch (Exception e2) {
                                    // 解析失败，跳过
                                    continue;
                                }
                            }

                            if (completedAt != null) {
                                long daysSinceAdoption = ChronoUnit.DAYS.between(completedAt, now);
                                if (daysSinceAdoption >= daysSinceUpdate) {
                                    isOverdue = true;
                                    daysOverdue = daysSinceAdoption;
                                    lastUpdateTime = "从未更新";
                                }
                            }
                        } catch (Exception e) {
                            // 解析时间失败，跳过
                            continue;
                        }
                    }
                } else {
                    // 有健康记录，检查最后更新时间
                    try {
                        String updatedAtStr = latestHealth.getUpdatedAt();
                        if (updatedAtStr != null && !updatedAtStr.isEmpty()) {
                            // 尝试多种时间格式解析
                            LocalDateTime lastUpdate = null;
                            try {
                                // 格式1: "yyyy-MM-dd HH:mm:ss"
                                DateTimeFormatter formatter1 = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
                                lastUpdate = LocalDateTime.parse(updatedAtStr, formatter1);
                            } catch (Exception e1) {
                                try {
                                    // 格式2: "yyyy-MM-ddTHH:mm:ss" (ISO格式)
                                    lastUpdate = LocalDateTime.parse(updatedAtStr.replace(" ", "T"));
                                } catch (Exception e2) {
                                    // 解析失败，跳过
                                    continue;
                                }
                            }

                            if (lastUpdate != null) {
                                long daysSinceLastUpdate = ChronoUnit.DAYS.between(lastUpdate, now);
                                if (daysSinceLastUpdate >= daysSinceUpdate) {
                                    isOverdue = true;
                                    daysOverdue = daysSinceLastUpdate;
                                    lastUpdateTime = updatedAtStr;
                                }
                            }
                        }
                    } catch (Exception e) {
                        // 解析时间失败，跳过
                        continue;
                    }
                }

                if (isOverdue) {
                    Map<String, Object> overdueInfo = new HashMap<>(adoptedPet);
                    overdueInfo.put("lastHealthUpdate", lastUpdateTime);
                    overdueInfo.put("daysOverdue", daysOverdue);
                    overdueInfo.put("petName", petMapper.findById(petId) != null ? petMapper.findById(petId).getName() : "未知");
                    overduePets.add(overdueInfo);
                }
            }

            return ApiResponse.success(overduePets);
        } catch (Exception e) {
            return ApiResponse.error(500, "查询逾期宠物失败: " + e.getMessage());
        }
    }

    /**
     * 发送逾期提醒通知给宠物主人
     *
     * 直接使用 RabbitMQ 发送消息，而不是跨服务调用
     *
     * @param petId 宠物ID
     * @param applicantId 领养人ID
     * @param daysOverdue 逾期天数
     */
    public ApiResponse<String> sendOverdueReminder(Long petId, Long applicantId, Long daysOverdue) {
        if (applicantId == null) {
            return ApiResponse.error(400, "领养人ID不能为空");
        }

        try {
            String title = "健康状态更新提醒";
            String body = String.format("您领养的宠物（ID: %d）已超过 %d 天未更新健康状态，请及时更新。", petId, daysOverdue);

            // 直接使用 RabbitMQ 发送消息
            notificationMessageService.sendSystemNotification(
                    applicantId,
                    title,
                    body,
                    "HEALTH_UPDATE_OVERDUE"
            );

            return ApiResponse.success("提醒通知已发送");
        } catch (Exception e) {
            return ApiResponse.error(500, "发送提醒失败: " + e.getMessage());
        }
    }
}

