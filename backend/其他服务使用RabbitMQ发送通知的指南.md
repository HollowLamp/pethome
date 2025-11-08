# 其他服务使用 RabbitMQ 发送通知的指南

## 概述

如果其他服务（如 `org-service`、`pet-service`、`interview-service` 等）也需要发送通知，应该采用与 `adoption-service` 类似的写法。

## 实现步骤

### 1. 添加依赖（如果还没有）

在服务的 `pom.xml` 中添加 RabbitMQ 依赖：

```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-amqp</artifactId>
</dependency>
```

### 2. 创建 RabbitMQ 配置类

在服务中创建 `RabbitMQConfig.java`，**只需要配置 Exchange，不需要配置 Queue**（因为 Queue 是在 `notification-service` 中定义的）。

```java
package com.adoption.xxx.config;  // 替换为实际的服务包名

import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMQConfig {

    // Exchange 名称（必须与 notification-service 中的一致）
    public static final String NOTIFY_EXCHANGE = "notify";

    /**
     * 创建 Topic Exchange（主题交换机）
     * 注意：如果多个服务都创建同名 Exchange，RabbitMQ 会复用同一个，不会重复创建
     */
    @Bean
    public TopicExchange notifyExchange() {
        return new TopicExchange(NOTIFY_EXCHANGE, true, false);
    }

    /**
     * JSON 消息转换器
     */
    @Bean
    public MessageConverter messageConverter() {
        return new Jackson2JsonMessageConverter();
    }

    /**
     * 配置 RabbitTemplate
     */
    @Bean
    public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory) {
        RabbitTemplate template = new RabbitTemplate(connectionFactory);
        template.setMessageConverter(messageConverter());
        return template;
    }
}
```

### 3. 创建消息通知服务类

创建 `NotificationMessageService.java`，封装发送消息的逻辑：

```java
package com.adoption.xxx.service;  // 替换为实际的服务包名

import com.adoption.xxx.config.RabbitMQConfig;  // 替换为实际的配置类路径
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

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

    /**
     * 发送私信通知
     */
    public void sendDirectMessage(Long fromUserId, Long toUserId, String content) {
        try {
            Map<String, Object> payload = new HashMap<>();
            payload.put("fromUserId", fromUserId);
            payload.put("toUserId", toUserId);
            payload.put("content", content);

            Map<String, Object> message = new HashMap<>();
            message.put("eventType", "notify.direct");
            message.put("payload", payload);

            String messageJson = objectMapper.writeValueAsString(message);
            rabbitTemplate.convertAndSend(
                    RabbitMQConfig.NOTIFY_EXCHANGE,
                    "notify.direct",
                    messageJson
            );

            log.info("私信通知已发送: fromUserId={}, toUserId={}", fromUserId, toUserId);
        } catch (Exception e) {
            log.error("发送私信通知失败: fromUserId={}, toUserId={}, error={}",
                    fromUserId, toUserId, e.getMessage(), e);
        }
    }

    /**
     * 发送点赞通知
     */
    public void sendLikeNotification(Long userId, String title, String body) {
        try {
            Map<String, Object> payload = new HashMap<>();
            payload.put("userId", userId);
            payload.put("title", title);
            payload.put("body", body);

            Map<String, Object> message = new HashMap<>();
            message.put("eventType", "notify.likes");
            message.put("payload", payload);

            String messageJson = objectMapper.writeValueAsString(message);
            rabbitTemplate.convertAndSend(
                    RabbitMQConfig.NOTIFY_EXCHANGE,
                    "notify.likes",
                    messageJson
            );

            log.info("点赞通知已发送: userId={}", userId);
        } catch (Exception e) {
            log.error("发送点赞通知失败: userId={}, error={}", userId, e.getMessage(), e);
        }
    }
}
```

### 4. 在业务代码中使用

在需要发送通知的业务代码中，注入 `NotificationMessageService` 并调用：

```java
@Service
public class YourBusinessService {

    @Autowired
    private NotificationMessageService notificationMessageService;

    public void someBusinessMethod() {
        // ... 业务逻辑 ...

        // 发送通知
        try {
            notificationMessageService.sendSystemNotification(
                    userId,
                    "通知标题",
                    "通知内容",
                    "TEMPLATE_CODE"  // 可选
            );
        } catch (Exception e) {
            // 消息发送失败不应该影响主业务流程，只记录日志
            System.err.println("发送通知失败: " + e.getMessage());
        }
    }
}
```

## 重要注意事项

### 1. Exchange 名称必须一致
- 所有服务使用的 Exchange 名称必须是 `"notify"`，与 `notification-service` 中的一致
- RabbitMQ 会自动复用同名 Exchange，不会重复创建

### 2. Routing Key 格式
- 系统通知：`"notify.system"`
- 私信通知：`"notify.direct"`
- 点赞通知：`"notify.likes"`
- 这些 Routing Key 必须与 `notification-service` 中队列的 Binding 规则匹配

### 3. 消息格式
消息必须是以下格式的 JSON：

```json
{
  "eventType": "notify.system",  // 或 "notify.direct"、"notify.likes"
  "payload": {
    "userId": 123,
    "title": "通知标题",
    "body": "通知内容",
    "templateCode": "TEMPLATE_CODE"  // 可选
  }
}
```

### 4. 错误处理
- 消息发送失败不应该影响主业务流程
- 应该使用 try-catch 捕获异常，只记录日志
- 参考 `adoption-service` 中的写法

### 5. 配置 RabbitMQ 连接
确保在 `application.yml` 或配置中心配置了 RabbitMQ 连接信息：

```yaml
spring:
  rabbitmq:
    host: localhost
    port: 5672
    username: guest
    password: guest
```

## 代码复用建议

如果多个服务都需要发送通知，可以考虑：

1. **提取为公共模块**：将 `NotificationMessageService` 提取到 `common` 模块中
2. **使用 Feign 调用**：通过 `notification-service` 提供的 REST API 发送通知（但这样会增加服务间耦合）
3. **保持当前方式**：每个服务都实现自己的 `NotificationMessageService`（代码重复但解耦）

**推荐方式**：如果通知逻辑简单且固定，可以提取到 `common` 模块；如果需要服务特定的逻辑（如获取角色用户列表），则保持在各服务中实现。

## 示例：org-service 发送通知

假设 `org-service` 需要在机构审核通过时发送通知：

```java
@Service
public class OrgService {

    @Autowired
    private NotificationMessageService notificationMessageService;

    public void approveOrg(Long orgId) {
        // ... 审核逻辑 ...

        // 发送通知给机构管理员
        try {
            notificationMessageService.sendSystemNotification(
                    adminUserId,
                    "机构审核已通过",
                    "恭喜！您的机构入驻申请已通过审核",
                    "ORG_APPROVED"
            );
        } catch (Exception e) {
            System.err.println("发送通知失败: " + e.getMessage());
        }
    }
}
```

