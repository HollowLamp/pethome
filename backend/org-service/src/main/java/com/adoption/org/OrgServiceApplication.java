package com.adoption.org;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.openfeign.EnableFeignClients;

/**
 * OrgService 应用入口
 *
 * 职责：
 * 1) 作为“机构组织服务”的启动类，负责引导 Spring Boot
 * 2) 开启服务注册发现（Eureka 客户端），使服务能够被网关/其他服务发现
 * 3) 预留 Feign 客户端能力，后续如需调用外部微服务可直接声明接口
 */
@SpringBootApplication
@EnableDiscoveryClient
@EnableFeignClients
public class OrgServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(OrgServiceApplication.class, args);
    }
}
