package com.adoption.community;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.openfeign.EnableFeignClients;

/**
 * 社区服务启动类
 * 
 * 作用：Spring Boot应用的入口点，负责启动和配置社区服务
 * 
 * 注解说明：
 * - @SpringBootApplication: Spring Boot应用主类，包含@Configuration、@EnableAutoConfiguration、@ComponentScan
 *   * scanBasePackages: 指定扫描的包路径，包括community和common包
 * - @EnableDiscoveryClient: 启用服务发现（如Eureka、Nacos），用于微服务注册和发现
 * - @EnableFeignClients: 启用Feign客户端，用于调用其他微服务（如auth-service）
 * 
 * 主要功能模块：
 * - 帖子管理（发布、查询、删除）
 * - 评论管理（发布、查询、删除）
 * - 点赞功能（帖子点赞、评论点赞）
 * - 举报功能（举报帖子、举报评论）
 * - 文件上传（图片、视频）
 * - 消息通知（通过RabbitMQ发送通知）
 * - 管理员功能（审核、推荐）
 */
@SpringBootApplication(scanBasePackages = {"com.adoption.community", "com.adoption.common"})
@EnableDiscoveryClient
@EnableFeignClients
public class CommunityServiceApplication {
    /**
     * 应用启动入口
     * 
     * @param args 命令行参数
     */
    public static void main(String[] args) {
        SpringApplication.run(CommunityServiceApplication.class, args);
    }
}

