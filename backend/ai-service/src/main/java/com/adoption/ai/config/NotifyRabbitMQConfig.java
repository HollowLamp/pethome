package com.adoption.ai.config;

import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 通知消息 RabbitMQ 配置类
 *
 * 用于发送通知消息（使用 notify Exchange）
 */
@Configuration
public class NotifyRabbitMQConfig {

    /**
     * Exchange 名称（与 notification-service 一致）
     */
    public static final String NOTIFY_EXCHANGE = "notify";

    /**
     * 创建 Topic Exchange（主题交换机）
     */
    @Bean
    public TopicExchange notifyExchange() {
        return new TopicExchange(NOTIFY_EXCHANGE, true, false);
    }

    /**
     * JSON 消息转换器
     */
    @Bean
    public MessageConverter notifyMessageConverter() {
        return new Jackson2JsonMessageConverter();
    }

    /**
     * 配置 RabbitTemplate（用于发送通知消息）
     */
    @Bean("notifyRabbitTemplate")
    public RabbitTemplate notifyRabbitTemplate(ConnectionFactory connectionFactory) {
        RabbitTemplate template = new RabbitTemplate(connectionFactory);
        template.setMessageConverter(notifyMessageConverter());
        return template;
    }
}

