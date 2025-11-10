package com.adoption.ai.config;

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
 * 用于接收 AI 分析请求
 */
@Configuration
public class RabbitMQConfig {

    /**
     * Exchange 名称
     */
    public static final String AI_EXCHANGE = "ai";

    /**
     * Queue 名称
     */
    public static final String AI_ANALYZE_QUEUE = "ai.analyze.queue";

    /**
     * Routing Key
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
     * 创建 AI 分析队列
     */
    @Bean
    public Queue aiAnalyzeQueue() {
        return QueueBuilder.durable(AI_ANALYZE_QUEUE).build();
    }

    /**
     * 绑定队列到 Exchange
     */
    @Bean
    public Binding aiAnalyzeBinding() {
        return BindingBuilder
                .bind(aiAnalyzeQueue())
                .to(aiExchange())
                .with(AI_ANALYZE_ROUTING_KEY);
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

