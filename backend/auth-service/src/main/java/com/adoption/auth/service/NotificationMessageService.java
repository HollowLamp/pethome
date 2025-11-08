package com.adoption.auth.service;

import com.adoption.auth.config.RabbitMQConfig;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

/**
 * 消息通知服务
 *
 * 封装 RabbitMQ 消息发送逻辑，提供简单易用的接口
 */
@Service
public class NotificationMessageService {

    private static final Logger log = LoggerFactory.getLogger(NotificationMessageService.class);

    @Autowired
    private RabbitTemplate rabbitTemplate;

    @Autowired
    private ObjectMapper objectMapper;

    /**
     * 发送系统通知消息
     *
     * @param userId 接收者用户ID
     * @param title 通知标题
     * @param body 通知内容
     * @param templateCode 模板代码（可选）
     */
    public void sendSystemNotification(Long userId, String title, String body, String templateCode) {
        try {
            // 构建消息负载
            Map<String, Object> payload = new HashMap<>();
            payload.put("userId", userId);
            payload.put("title", title);
            payload.put("body", body);
            if (templateCode != null) {
                payload.put("templateCode", templateCode);
            }

            // 构建完整消息
            Map<String, Object> message = new HashMap<>();
            message.put("eventType", "notify.system");
            message.put("payload", payload);

            // 转换为 JSON 并发送
            String messageJson = objectMapper.writeValueAsString(message);
            rabbitTemplate.convertAndSend(
                    RabbitMQConfig.NOTIFY_EXCHANGE,
                    "notify.system",
                    messageJson
            );

            log.info("系统通知已发送: userId={}, title={}", userId, title);
        } catch (Exception e) {
            log.error("发送系统通知失败: userId={}, error={}", userId, e.getMessage(), e);
        }
    }

    /**
     * 发送系统通知（简化版）
     */
    public void sendSystemNotification(Long userId, String title, String body) {
        sendSystemNotification(userId, title, body, null);
    }
}

