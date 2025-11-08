package com.adoption.notification.config;

import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMQConfig {

    // Exchange 名称
    public static final String NOTIFY_EXCHANGE = "notify";

    // Queue 名称
    public static final String NOTIFY_QUEUE = "notify.queue";

    // Routing Key 模式
    public static final String NOTIFY_ROUTING_KEY = "notify.*";

    /**
     * 创建 Topic Exchange
     */
    @Bean
    public TopicExchange notifyExchange() {
        return new TopicExchange(NOTIFY_EXCHANGE, true, false);
    }

    /**
     * 创建通知队列
     */
    @Bean
    public Queue notifyQueue() {
        return QueueBuilder.durable(NOTIFY_QUEUE).build();
    }

    /**
     * 绑定队列到 Exchange
     */
    @Bean
    public Binding notifyBinding() {
        return BindingBuilder
                .bind(notifyQueue())
                .to(notifyExchange())
                .with(NOTIFY_ROUTING_KEY);
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

