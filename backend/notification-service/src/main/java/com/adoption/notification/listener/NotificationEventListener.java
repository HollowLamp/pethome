package com.adoption.notification.listener;

import com.adoption.notification.config.RabbitMQConfig;
import com.adoption.notification.model.InboxMessage;
import com.adoption.notification.model.NotifyTask;
import com.adoption.notification.service.NotificationService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * 通知事件监听器
 * 监听 notify.* 事件
 */
@Component
public class NotificationEventListener {

    private static final Logger log = LoggerFactory.getLogger(NotificationEventListener.class);

    @Autowired
    private NotificationService notificationService;

    @Autowired
    private ObjectMapper objectMapper;

    /**
     * 监听 notify.* 事件
     */
    @RabbitListener(queues = RabbitMQConfig.NOTIFY_QUEUE)
    public void handleNotificationEvent(String message) {
        try {
            log.info("收到通知事件: {}", message);

            // 解析消息
            Map<String, Object> eventData = objectMapper.readValue(message, Map.class);
            String eventType = (String) eventData.get("eventType");
            Map<String, Object> payload = (Map<String, Object>) eventData.get("payload");

            if (payload == null) {
                log.warn("事件负载为空，忽略处理");
                return;
            }

            // 获取用户ID
            Object userIdObj = payload.get("userId");
            if (userIdObj == null) {
                log.warn("事件中缺少 userId，忽略处理");
                return;
            }
            Long userId = Long.valueOf(userIdObj.toString());

            // 根据事件类型处理
            switch (eventType != null ? eventType : "") {
                case "notify.system":
                    handleSystemNotification(userId, payload);
                    break;
                case "notify.direct":
                    handleDirectNotification(userId, payload);
                    break;
                case "notify.likes":
                    handleLikeNotification(userId, payload);
                    break;
                default:
                    // 默认处理为系统通知
                    handleSystemNotification(userId, payload);
                    break;
            }

        } catch (Exception e) {
            log.error("处理通知事件失败: {}", e.getMessage(), e);
        }
    }

    /**
     * 处理系统通知
     */
    private void handleSystemNotification(Long userId, Map<String, Object> payload) {
        try {
            String templateCode = (String) payload.get("templateCode");
            String channel = (String) payload.getOrDefault("channel", "SYSTEM");
            String payloadJson = objectMapper.writeValueAsString(payload);

            // 创建通知任务
            NotifyTask task = notificationService.createNotifyTask(
                    userId,
                    channel,
                    templateCode,
                    payloadJson
            );

            // 同时创建收件箱消息
            String title = (String) payload.getOrDefault("title", "系统通知");
            String body = (String) payload.getOrDefault("body", "");
            notificationService.createInboxMessage(userId, title, body);

            // 标记任务为已发送
            task.setStatus("SENT");
            notificationService.updateNotifyTaskStatus(task);
            log.info("系统通知已创建: userId={}, taskId={}", userId, task.getId());

        } catch (Exception e) {
            log.error("处理系统通知失败: {}", e.getMessage(), e);
        }
    }

    /**
     * 处理私信通知
     */
    private void handleDirectNotification(Long userId, Map<String, Object> payload) {
        try {
            Object fromUserIdObj = payload.get("fromUserId");
            String content = (String) payload.get("content");

            if (fromUserIdObj == null || content == null) {
                log.warn("私信通知参数不完整");
                return;
            }

            Long fromUserId = Long.valueOf(fromUserIdObj.toString());
            notificationService.sendDirectMessage(fromUserId, userId, content);

            log.info("私信通知已处理: fromUserId={}, toUserId={}", fromUserId, userId);

        } catch (Exception e) {
            log.error("处理私信通知失败: {}", e.getMessage(), e);
        }
    }

    /**
     * 处理点赞通知
     */
    private void handleLikeNotification(Long userId, Map<String, Object> payload) {
        try {
            String title = (String) payload.getOrDefault("title", "点赞通知");
            String body = (String) payload.getOrDefault("body", "有人给你点赞了");

            notificationService.createInboxMessage(userId, title, body);

            log.info("点赞通知已创建: userId={}", userId);

        } catch (Exception e) {
            log.error("处理点赞通知失败: {}", e.getMessage(), e);
        }
    }
}