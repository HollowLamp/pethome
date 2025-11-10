package com.adoption.ai.service;

import com.adoption.ai.config.NotifyRabbitMQConfig;
import com.adoption.ai.feign.AuthServiceClient;
import com.adoption.common.api.ApiResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 消息通知服务
 *
 * 作用：封装 RabbitMQ 消息发送逻辑，提供简单易用的接口
 *
 * 使用场景：
 * - AI 内容审核发现违规时，通知帖子作者
 * - AI 成功更新宠物健康状态时，通知用户
 * - AI 内容总结完成时，可以通知用户（可选）
 */
@Service
public class NotificationMessageService {
    private static final Logger log = LoggerFactory.getLogger(NotificationMessageService.class);

    @Autowired
    @Qualifier("notifyRabbitTemplate")
    private RabbitTemplate notifyRabbitTemplate;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private AuthServiceClient authServiceClient;

    /**
     * 发送系统通知消息
     *
     * @param userId 接收者用户ID
     * @param title 通知标题
     * @param body 通知内容
     * @param templateCode 模板代码（可选，用于消息模板匹配）
     */
    public void sendSystemNotification(Long userId, String title, String body, String templateCode) {
        try {
            // 构建消息负载（payload）
            Map<String, Object> payload = new HashMap<>();
            payload.put("userId", userId);
            payload.put("title", title);
            payload.put("body", body);
            if (templateCode != null) {
                payload.put("templateCode", templateCode);
            }

            // 构建完整消息
            Map<String, Object> message = new HashMap<>();
            message.put("eventType", "notify.system"); // 事件类型：系统通知
            message.put("payload", payload);

            // 将消息对象转换为 JSON 字符串
            String messageJson = objectMapper.writeValueAsString(message);

            // 发送消息到 RabbitMQ
            notifyRabbitTemplate.convertAndSend(
                    NotifyRabbitMQConfig.NOTIFY_EXCHANGE,
                    "notify.system",
                    messageJson
            );

            log.info("系统通知已发送: userId={}, title={}", userId, title);

        } catch (Exception e) {
            // 消息发送失败不应该影响主业务流程，只记录日志
            log.error("发送系统通知失败: userId={}, error={}", userId, e.getMessage(), e);
        }
    }

    /**
     * 发送系统通知消息（简化版，只传必要参数）
     *
     * @param userId 接收者用户ID
     * @param title 通知标题
     * @param body 通知内容
     */
    public void sendSystemNotification(Long userId, String title, String body) {
        sendSystemNotification(userId, title, body, null);
    }

    /**
     * 根据角色获取用户ID列表
     *
     * 通过调用 auth-service 获取所有拥有指定角色的用户ID
     *
     * @param role 角色名称（如：CS, ADMIN, AUDITOR）
     * @return 用户ID列表
     */
    private List<Long> getUserIdsByRole(String role) {
        List<Long> userIds = new ArrayList<>();
        try {
            // 分页获取用户列表，每页100条
            int page = 1;
            int pageSize = 100;
            boolean hasMore = true;

            while (hasMore) {
                ApiResponse<Map<String, Object>> response = authServiceClient.getUserList(page, pageSize);

                if (response != null && response.getCode() == 200 && response.getData() != null) {
                    Map<String, Object> data = response.getData();
                    @SuppressWarnings("unchecked")
                    List<Map<String, Object>> userList = (List<Map<String, Object>>) data.get("list");

                    if (userList != null) {
                        // 遍历用户列表，筛选出拥有指定角色的用户
                        for (Map<String, Object> user : userList) {
                            @SuppressWarnings("unchecked")
                            List<String> roles = (List<String>) user.get("roles");

                            if (roles != null && roles.contains(role)) {
                                Object idObj = user.get("id");
                                if (idObj != null) {
                                    Long userId = idObj instanceof Long ? (Long) idObj : Long.valueOf(idObj.toString());
                                    userIds.add(userId);
                                }
                            }
                        }
                    }

                    // 检查是否还有更多数据
                    Integer total = (Integer) data.get("total");
                    int currentCount = page * pageSize;
                    hasMore = total != null && currentCount < total;
                    page++;
                } else {
                    hasMore = false;
                }
            }

            log.info("获取角色用户列表: role={}, count={}", role, userIds.size());
        } catch (Exception e) {
            log.error("获取角色用户列表失败: role={}, error={}", role, e.getMessage(), e);
        }
        return userIds;
    }

    /**
     * 批量发送系统通知给指定角色的所有用户
     *
     * @param role 角色名称
     * @param title 通知标题
     * @param body 通知内容
     * @param templateCode 模板代码（可选）
     */
    public void sendSystemNotificationToRole(String role, String title, String body, String templateCode) {
        try {
            // 获取拥有该角色的所有用户ID
            List<Long> userIds = getUserIdsByRole(role);

            if (userIds.isEmpty()) {
                log.warn("角色 {} 没有用户，跳过发送通知", role);
                return;
            }

            // 批量发送通知给每个用户
            for (Long userId : userIds) {
                sendSystemNotification(userId, title, body, templateCode);
            }

            log.info("批量发送系统通知完成: role={}, count={}, title={}", role, userIds.size(), title);
        } catch (Exception e) {
            log.error("批量发送系统通知失败: role={}, error={}", role, e.getMessage(), e);
        }
    }

    /**
     * 批量发送系统通知给指定角色的所有用户（简化版）
     *
     * @param role 角色名称
     * @param title 通知标题
     * @param body 通知内容
     */
    public void sendSystemNotificationToRole(String role, String title, String body) {
        sendSystemNotificationToRole(role, title, body, null);
    }
}

