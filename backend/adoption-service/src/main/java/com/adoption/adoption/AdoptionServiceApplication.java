package com.adoption.adoption;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.context.annotation.ComponentScan;

@SpringBootApplication
@EnableDiscoveryClient  // 开启 Eureka 注册
@ComponentScan(basePackages = {"com.adoption.adoption", "com.adoption.common"})
public class AdoptionServiceApplication {
    public static void main(String[] args) {
        SpringApplication.run(AdoptionServiceApplication.class, args);
    }
}