package com.adoption.ai;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.openfeign.EnableFeignClients;

/**
 * AI 服务启动类
 */
@SpringBootApplication(scanBasePackages = {"com.adoption.ai", "com.adoption.common"})
@EnableDiscoveryClient
@EnableFeignClients
public class AiServiceApplication {
    public static void main(String[] args) {
        SpringApplication.run(AiServiceApplication.class, args);
    }
}

