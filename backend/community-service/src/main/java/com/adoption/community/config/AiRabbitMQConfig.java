package com.adoption.community.config;

import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * AI 分析 RabbitMQ 配置类
 *
 * 用于异步发送 AI 分析请求
 */
@Configuration
public class AiRabbitMQConfig {

    /**
     * Exchange 名称
     */
    public static final String AI_EXCHANGE = "ai";

    /**
     * Routing Key
     * ai.analyze - AI 分析请求
     */
    public static final String AI_ANALYZE_ROUTING_KEY = "ai.analyze";

    /**
     * 创建 Topic Exchange
     */
    @Bean
    public TopicExchange aiExchange() {
        return new TopicExchange(AI_EXCHANGE, true, false);
    }

    /**
     * JSON 消息转换器
     */
    @Bean
    public MessageConverter aiMessageConverter() {
        return new Jackson2JsonMessageConverter();
    }

    /**
     * 配置 RabbitTemplate（用于发送 AI 分析请求）
     */
    @Bean("aiRabbitTemplate")
    public RabbitTemplate aiRabbitTemplate(ConnectionFactory connectionFactory) {
        RabbitTemplate template = new RabbitTemplate(connectionFactory);
        template.setMessageConverter(aiMessageConverter());
        return template;
    }
}

