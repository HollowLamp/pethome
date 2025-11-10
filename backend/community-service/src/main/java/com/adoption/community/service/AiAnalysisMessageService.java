package com.adoption.community.service;

import com.adoption.community.config.AiRabbitMQConfig;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

/**
 * AI 分析消息服务
 *
 * 作用：通过 RabbitMQ 异步发送 AI 分析请求
 */
@Service
public class AiAnalysisMessageService {
    private static final Logger log = LoggerFactory.getLogger(AiAnalysisMessageService.class);

    @Autowired
    @Qualifier("aiRabbitTemplate")
    private RabbitTemplate aiRabbitTemplate;

    @Autowired
    private ObjectMapper objectMapper;

    /**
     * 发送内容审核请求
     */
    public void sendContentModerationRequest(Long postId, String title, String content, Long authorId) {
        try {
            Map<String, Object> message = new HashMap<>();
            message.put("type", "CONTENT_MOD");
            message.put("postId", postId);
            message.put("title", title);
            message.put("content", content);
            message.put("authorId", authorId); // 添加作者ID，用于发送通知

            String messageJson = objectMapper.writeValueAsString(message);
            aiRabbitTemplate.convertAndSend(
                    AiRabbitMQConfig.AI_EXCHANGE,
                    AiRabbitMQConfig.AI_ANALYZE_ROUTING_KEY,
                    messageJson
            );

            log.info("AI 内容审核请求已发送: postId={}, authorId={}", postId, authorId);
        } catch (Exception e) {
            log.error("发送 AI 内容审核请求失败: postId={}, error={}", postId, e.getMessage(), e);
        }
    }

    /**
     * 发送内容总结请求
     */
    public void sendSummaryRequest(Long postId, String title, String content, Long authorId) {
        try {
            Map<String, Object> message = new HashMap<>();
            message.put("type", "SUMMARY");
            message.put("postId", postId);
            message.put("title", title);
            message.put("content", content);
            message.put("authorId", authorId); // 添加作者ID，用于发送通知

            String messageJson = objectMapper.writeValueAsString(message);
            aiRabbitTemplate.convertAndSend(
                    AiRabbitMQConfig.AI_EXCHANGE,
                    AiRabbitMQConfig.AI_ANALYZE_ROUTING_KEY,
                    messageJson
            );

            log.info("AI 内容总结请求已发送: postId={}, authorId={}", postId, authorId);
        } catch (Exception e) {
            log.error("发送 AI 内容总结请求失败: postId={}, error={}", postId, e.getMessage(), e);
        }
    }

    /**
     * 发送状态提取请求
     */
    public void sendStateExtractRequest(Long postId, String content, Long bindPetId, Long authorId) {
        try {
            Map<String, Object> message = new HashMap<>();
            message.put("type", "STATE_EXTRACT");
            message.put("postId", postId);
            message.put("content", content);
            message.put("bindPetId", bindPetId);
            message.put("authorId", authorId);

            String messageJson = objectMapper.writeValueAsString(message);
            aiRabbitTemplate.convertAndSend(
                    AiRabbitMQConfig.AI_EXCHANGE,
                    AiRabbitMQConfig.AI_ANALYZE_ROUTING_KEY,
                    messageJson
            );

            log.info("AI 状态提取请求已发送: postId={}, bindPetId={}", postId, bindPetId);
        } catch (Exception e) {
            log.error("发送 AI 状态提取请求失败: postId={}, error={}", postId, e.getMessage(), e);
        }
    }
}

