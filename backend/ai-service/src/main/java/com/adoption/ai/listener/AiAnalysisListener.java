package com.adoption.ai.listener;

import com.adoption.ai.config.RabbitMQConfig;
import com.adoption.ai.service.AiService;
import com.adoption.ai.service.NotificationMessageService;
import com.adoption.ai.feign.CommunityServiceClient;
import com.adoption.ai.feign.AdoptionServiceClient;
import com.adoption.ai.feign.PetServiceClient;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

/**
 * AI 分析消息监听器
 *
 * 监听 AI 分析请求并处理
 */
@Component
public class AiAnalysisListener {
    private static final Logger log = LoggerFactory.getLogger(AiAnalysisListener.class);

    @Autowired
    private AiService aiService;

    @Autowired
    private CommunityServiceClient communityServiceClient;

    @Autowired
    private AdoptionServiceClient adoptionServiceClient;

    @Autowired
    private PetServiceClient petServiceClient;

    @Autowired
    private NotificationMessageService notificationMessageService;

    @Autowired
    private ObjectMapper objectMapper;

    /**
     * 监听 AI 分析请求
     */
    @RabbitListener(queues = RabbitMQConfig.AI_ANALYZE_QUEUE)
    public void handleAiAnalysisRequest(String message) {
        try {
            log.info("收到 AI 分析请求: {}", message);

            // 解析消息
            Map<String, Object> request = objectMapper.readValue(message, Map.class);
            String type = (String) request.get("type");
            Long postId = Long.valueOf(request.get("postId").toString());

            if (type == null) {
                log.warn("AI 分析请求缺少类型，忽略处理");
                return;
            }

            // 根据类型处理不同的 AI 分析
            switch (type) {
                case "CONTENT_MOD":
                    handleContentModeration(request);
                    break;
                case "SUMMARY":
                    handleSummary(request);
                    break;
                case "STATE_EXTRACT":
                    handleStateExtract(request);
                    break;
                default:
                    log.warn("未知的 AI 分析类型: {}", type);
                    break;
            }

        } catch (Exception e) {
            log.error("处理 AI 分析请求失败: {}", e.getMessage(), e);
        }
    }

    /**
     * 处理内容审核
     */
    private void handleContentModeration(Map<String, Object> request) {
        Long postId = null;
        String title = null;
        String content = null;
        Object authorIdObj = null;

        try {
            // 解析请求参数（容错处理）
            postId = Long.valueOf(request.get("postId").toString());
            title = (String) request.get("title");
            content = (String) request.get("content");
            authorIdObj = request.get("authorId");
        } catch (Exception e) {
            log.error("解析内容审核请求参数失败: request={}, error={}", request, e.getMessage(), e);
            return; // 参数解析失败，直接返回，不影响其他处理
        }

        // 调用 AI 服务进行内容审核（容错处理）
        com.adoption.common.api.ApiResponse<Boolean> response = null;
        try {
            response = aiService.checkContentModeration(postId, title, content);
        } catch (Exception e) {
            log.error("AI 内容审核调用失败: postId={}, error={}", postId, e.getMessage(), e);
            return; // AI 调用失败，直接返回，不影响其他处理
        }

        if (response == null || response.getCode() != 200 || response.getData() == null) {
            log.warn("AI 内容审核返回异常: postId={}, response={}", postId, response);
            return; // AI 返回异常，直接返回
        }

        Boolean isFlagged = response.getData();
        log.info("AI 内容审核完成: postId={}, flagged={}", postId, isFlagged);

        // 通过 Feign 调用 community-service 更新帖子状态（容错处理）
        try {
            Map<String, Object> updateRequest = new java.util.HashMap<>();
            updateRequest.put("postId", postId);
            updateRequest.put("aiFlagged", isFlagged);
            if (isFlagged) {
                updateRequest.put("status", "FLAGGED");
            }

            com.adoption.common.api.ApiResponse<String> updateResponse =
                communityServiceClient.updatePostAiStatus(updateRequest);

            if (updateResponse != null && updateResponse.getCode() == 200) {
                log.info("帖子 AI 状态更新成功: postId={}, flagged={}", postId, isFlagged);
            } else {
                log.warn("帖子 AI 状态更新失败: postId={}, response={}", postId, updateResponse);
            }
        } catch (Exception e) {
            // Feign 调用失败不影响后续通知发送
            log.error("更新帖子 AI 状态失败: postId={}, error={}", postId, e.getMessage(), e);
        }

        // 如果内容被标记为违规，通知帖子作者和客服人员（容错处理，互不影响）
        if (isFlagged) {
            // 通知帖子作者（独立容错）
            if (authorIdObj != null) {
                try {
                    Long authorId = Long.valueOf(authorIdObj.toString());
                    notificationMessageService.sendSystemNotification(
                            authorId,
                            "帖子内容审核提醒",
                            String.format("您的帖子「%s」（ID: %d）已被 AI 标记为可能违规，需要人工审核。请检查内容是否符合社区规范。",
                                title != null ? title : "未命名", postId),
                            "AI_CONTENT_FLAGGED"
                    );
                    log.info("违规内容通知已发送给作者: postId={}, authorId={}", postId, authorId);
                } catch (Exception e) {
                    // 通知发送失败不影响其他处理
                    log.error("发送违规内容通知给作者失败: postId={}, error={}", postId, e.getMessage(), e);
                }
            }

            // 通知客服人员（CS角色）有新的违规内容需要审核（独立容错）
            try {
                notificationMessageService.sendSystemNotificationToRole(
                        "CS",
                        "新的违规内容待审核",
                        String.format("AI 检测到新的违规内容：帖子「%s」（ID: %d），请及时审核处理。",
                            title != null ? title : "未命名", postId),
                        "AI_CONTENT_FLAGGED_TO_CS"
                );
                log.info("违规内容通知已发送给客服: postId={}", postId);
            } catch (Exception e) {
                // 通知发送失败不影响其他处理
                log.error("发送违规内容通知给客服失败: postId={}, error={}", postId, e.getMessage(), e);
            }
        }
    }

    /**
     * 处理内容总结
     */
    private void handleSummary(Map<String, Object> request) {
        Long postId = null;
        String title = null;
        String content = null;
        Object authorIdObj = null;

        try {
            // 解析请求参数（容错处理）
            postId = Long.valueOf(request.get("postId").toString());
            title = (String) request.get("title");
            content = (String) request.get("content");
            authorIdObj = request.get("authorId");
        } catch (Exception e) {
            log.error("解析内容总结请求参数失败: request={}, error={}", request, e.getMessage(), e);
            return; // 参数解析失败，直接返回
        }

        // 调用 AI 服务生成总结（容错处理）
        com.adoption.common.api.ApiResponse<String> response = null;
        try {
            response = aiService.generateSummary(postId, title, content);
        } catch (Exception e) {
            log.error("AI 内容总结调用失败: postId={}, error={}", postId, e.getMessage(), e);
            return; // AI 调用失败，直接返回
        }

        if (response == null || response.getCode() != 200 || response.getData() == null) {
            log.warn("AI 内容总结返回异常: postId={}, response={}", postId, response);
            return; // AI 返回异常，直接返回
        }

        String summary = response.getData();
        log.info("AI 内容总结完成: postId={}, summaryLength={}", postId, summary != null ? summary.length() : 0);

        // 通过 Feign 调用 community-service 更新帖子总结（容错处理）
        try {
            Map<String, Object> updateRequest = new java.util.HashMap<>();
            updateRequest.put("postId", postId);
            updateRequest.put("aiSummary", summary);

            com.adoption.common.api.ApiResponse<String> updateResponse =
                communityServiceClient.updatePostAiSummary(updateRequest);

            if (updateResponse != null && updateResponse.getCode() == 200) {
                log.info("帖子 AI 总结更新成功: postId={}", postId);
            } else {
                log.warn("帖子 AI 总结更新失败: postId={}, response={}", postId, updateResponse);
            }
        } catch (Exception e) {
            // Feign 调用失败不影响其他处理
            log.error("更新帖子 AI 总结失败: postId={}, error={}", postId, e.getMessage(), e);
        }

        // 可选：通知用户内容总结已完成（如果需要的话）
        // 这里暂时不发送通知，因为总结是自动完成的，用户可能不需要特别通知
        // 如果需要，可以取消下面的注释
        /*
        if (authorIdObj != null) {
            try {
                Long authorId = Long.valueOf(authorIdObj.toString());
                notificationMessageService.sendSystemNotification(
                        authorId,
                        "内容总结已生成",
                        String.format("您的养宠攻略「%s」的 AI 总结已生成。", title != null ? title : "未命名"),
                        "AI_SUMMARY_GENERATED"
                );
                log.info("内容总结通知已发送: postId={}, authorId={}", postId, authorId);
            } catch (Exception e) {
                // 通知发送失败不影响其他处理
                log.error("发送内容总结通知失败: postId={}, error={}", postId, e.getMessage(), e);
            }
        }
        */
    }

    /**
     * 处理状态提取
     */
    private void handleStateExtract(Map<String, Object> request) {
        Long postId = null;
        String content = null;
        Object bindPetIdObj = null;
        Object authorIdObj = null;

        try {
            // 解析请求参数（容错处理）
            postId = Long.valueOf(request.get("postId").toString());
            content = (String) request.get("content");
            bindPetIdObj = request.get("bindPetId");
            authorIdObj = request.get("authorId");
        } catch (Exception e) {
            log.error("解析状态提取请求参数失败: request={}, error={}", request, e.getMessage(), e);
            return; // 参数解析失败，直接返回
        }

        // 调用 AI 服务提取状态（容错处理）
        com.adoption.common.api.ApiResponse<Map<String, Object>> response = null;
        try {
            Long bindPetId = null;
            if (bindPetIdObj != null) {
                try {
                    bindPetId = Long.valueOf(bindPetIdObj.toString());
                } catch (Exception e) {
                    log.warn("解析bindPetId失败，将使用null: bindPetIdObj={}, error={}", bindPetIdObj, e.getMessage());
                }
            }
            response = aiService.extractPetState(postId, content, bindPetId);
        } catch (Exception e) {
            log.error("AI 状态提取调用失败: postId={}, error={}", postId, e.getMessage(), e);
            return; // AI 调用失败，直接返回
        }

        if (response == null || response.getCode() != 200 || response.getData() == null) {
            log.warn("AI 状态提取返回异常: postId={}, response={}", postId, response);
            return; // AI 返回异常，直接返回
        }

        Map<String, Object> stateData = response.getData();
        log.info("AI 状态提取完成: postId={}, stateKeys={}", postId, stateData != null ? stateData.keySet() : "null");

        // 如果帖子未绑定宠物ID，直接返回（不需要更新健康状态）
        if (bindPetIdObj == null || authorIdObj == null) {
            log.info("帖子未绑定宠物ID或作者ID，跳过健康状态更新: postId={}, bindPetId={}, authorId={}",
                postId, bindPetIdObj, authorIdObj);
            return;
        }

        // 尝试自动更新宠物健康状态（容错处理，每个步骤独立容错）
        Long bindPetId = null;
        Long authorId = null;
        try {
            bindPetId = Long.valueOf(bindPetIdObj.toString());
            authorId = Long.valueOf(authorIdObj.toString());
        } catch (Exception e) {
            log.error("解析宠物ID或作者ID失败: bindPetIdObj={}, authorIdObj={}, error={}",
                bindPetIdObj, authorIdObj, e.getMessage(), e);
            return; // ID 解析失败，直接返回
        }

        // 1. 验证用户是否领养了该宠物（容错处理）
        boolean isOwner = false;
        try {
            com.adoption.common.api.ApiResponse<Boolean> ownershipResponse =
                adoptionServiceClient.checkOwnership(bindPetId, authorId);

            if (ownershipResponse != null && ownershipResponse.getCode() == 200
                && Boolean.TRUE.equals(ownershipResponse.getData())) {
                isOwner = true;
                log.info("验证宠物所有权成功: petId={}, authorId={}", bindPetId, authorId);
            } else {
                log.info("用户未领养该宠物，跳过健康状态更新: petId={}, authorId={}, response={}",
                    bindPetId, authorId, ownershipResponse);
            }
        } catch (Exception e) {
            // 所有权验证失败不影响后续处理，但跳过更新
            log.error("验证宠物所有权失败: petId={}, authorId={}, error={}",
                bindPetId, authorId, e.getMessage(), e);
            return; // 验证失败，直接返回
        }

        if (!isOwner) {
            return; // 不是宠物主人，跳过更新
        }

        // 2. 构建健康状态对象（匹配 PetHealth 模型，容错处理）
        Map<String, Object> healthData = new HashMap<>();
        try {
            // weight: BigDecimal
            Object weightObj = stateData.get("weight");
            if (weightObj != null) {
                if (weightObj instanceof BigDecimal) {
                    healthData.put("weight", weightObj);
                } else if (weightObj instanceof Number) {
                    healthData.put("weight", new BigDecimal(weightObj.toString()));
                }
            }

            // vaccine: String (JSON字符串)
            Object vaccineObj = stateData.get("vaccine");
            if (vaccineObj != null) {
                healthData.put("vaccine", vaccineObj.toString());
            }

            // note: String
            Object noteObj = stateData.get("note");
            if (noteObj != null) {
                healthData.put("note", noteObj.toString());
            }
        } catch (Exception e) {
            log.error("构建健康状态数据失败: petId={}, stateData={}, error={}",
                bindPetId, stateData, e.getMessage(), e);
            return; // 数据构建失败，直接返回
        }

        // 3. 调用 pet-service 更新健康状态（容错处理）
        try {
            com.adoption.common.api.ApiResponse<Map<String, Object>> updateResponse =
                petServiceClient.updateHealthByOwner(authorId, bindPetId, healthData);

            if (updateResponse != null && updateResponse.getCode() == 200) {
                log.info("宠物健康状态更新成功: petId={}, authorId={}", bindPetId, authorId);

                // 发送通知给用户：AI 已自动更新宠物健康状态（独立容错）
                try {
                    notificationMessageService.sendSystemNotification(
                            authorId,
                            "宠物健康状态已更新",
                            String.format("AI 已从您的养宠日常帖子中提取并更新了宠物（ID: %d）的健康状态。", bindPetId),
                            "AI_HEALTH_UPDATED"
                    );
                    log.info("健康状态更新通知已发送: petId={}, authorId={}", bindPetId, authorId);
                } catch (Exception e) {
                    // 通知发送失败不影响主流程
                    log.error("发送健康状态更新通知失败: petId={}, authorId={}, error={}",
                        bindPetId, authorId, e.getMessage(), e);
                }
            } else {
                log.warn("宠物健康状态更新失败: petId={}, authorId={}, response={}",
                    bindPetId, authorId, updateResponse);
            }
        } catch (Exception e) {
            // 健康状态更新失败不影响其他处理
            log.error("更新宠物健康状态失败: petId={}, authorId={}, error={}",
                bindPetId, authorId, e.getMessage(), e);
        }
    }
}

