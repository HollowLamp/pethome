package com.adoption.adoption.config;

import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * RabbitMQ 配置类
 *
 * RabbitMQ 是一个消息队列中间件，用于在微服务之间异步传递消息。
 *
 * 核心概念：
 * 1. Exchange（交换机）：接收消息并根据路由规则将消息转发到队列
 *    - TopicExchange：主题交换机，支持通配符路由（如 notify.*）
 * 2. Queue（队列）：存储消息的地方
 * 3. Binding（绑定）：将 Exchange 和 Queue 关联起来，并指定路由规则
 * 4. Routing Key（路由键）：用于匹配消息路由规则的关键字
 *
 * 工作流程：
 * 生产者（adoption-service） -> Exchange -> 根据 Routing Key -> Queue -> 消费者（notification-service）
 */
@Configuration
public class RabbitMQConfig {

    /**
     * Exchange 名称
     * Exchange 是消息的接收者，负责将消息路由到相应的队列
     */
    public static final String NOTIFY_EXCHANGE = "notify";

    /**
     * Routing Key 前缀
     * 使用 TopicExchange，支持通配符匹配
     * notify.system - 系统通知
     * notify.direct - 私信通知
     * notify.likes - 点赞通知
     */
    public static final String ROUTING_KEY_PREFIX = "notify.";

    /**
     * 创建 Topic Exchange（主题交换机）
     *
     * TopicExchange 特点：
     * - 支持通配符路由：* 匹配一个单词，# 匹配零个或多个单词
     * - 例如：notify.* 可以匹配 notify.system、notify.direct 等
     *
     * 参数说明：
     * - name: Exchange 名称
     * - durable: true 表示持久化，服务器重启后 Exchange 仍然存在
     * - autoDelete: false 表示当没有队列绑定时不会自动删除
     */
    @Bean
    public TopicExchange notifyExchange() {
        return new TopicExchange(NOTIFY_EXCHANGE, true, false);
    }

    /**
     * JSON 消息转换器
     *
     * 作用：将 Java 对象转换为 JSON 格式发送到队列，接收时再转换回 Java 对象
     * 这样我们就可以直接发送对象，而不需要手动序列化
     */
    @Bean
    public MessageConverter messageConverter() {
        return new Jackson2JsonMessageConverter();
    }

    /**
     * 配置 RabbitTemplate（RabbitMQ 操作模板）
     *
     * RabbitTemplate 是 Spring AMQP 提供的操作 RabbitMQ 的工具类
     * 类似于 JdbcTemplate，封装了发送和接收消息的常用操作
     *
     * 使用方式：
     * rabbitTemplate.convertAndSend(exchange, routingKey, message)
     *
     * @param connectionFactory RabbitMQ 连接工厂（Spring Boot 自动配置）
     * @return 配置好的 RabbitTemplate
     */
    @Bean
    public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory) {
        RabbitTemplate template = new RabbitTemplate(connectionFactory);
        // 设置消息转换器，自动将对象转为 JSON
        template.setMessageConverter(messageConverter());
        return template;
    }
}

