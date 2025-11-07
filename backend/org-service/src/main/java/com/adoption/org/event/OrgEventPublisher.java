package com.adoption.org.event;

import org.springframework.stereotype.Component;

/**
 * 事件发布器（占位实现）
 * 先用 System.out 模拟事件发布，后续替换为 RabbitMQ/Kafka/Spring Cloud Stream 即可
 */
@Component  // ← 这是关键，让它成为 Spring Bean，能被注入
public class OrgEventPublisher {

    /**
     * 发布组织事件
     * @param event 事件类型
     * @param orgId 机构ID
     */
    public void publish(OrgEvent event, Long orgId) {
        // TODO: 未来这里接入消息队列
        System.out.println("[EVENT] " + event + " : orgId=" + orgId);
    }
}

