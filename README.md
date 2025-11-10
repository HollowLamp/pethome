# 🐾 PetHome 宠物领养平台

[![微服务架构](https://img.shields.io/badge/Architecture-Microservices-blue)](https://spring.io/projects/spring-cloud) [![Spring Boot 3.2.2](https://img.shields.io/badge/Spring%20Boot-3.2.2-green)](https://spring.io/projects/spring-boot) [![License](https://img.shields.io/badge/License-Apache%202.0-orange.svg)](https://www.apache.org/licenses/LICENSE-2.0)

PetHome 是一个基于 **Spring Cloud 微服务架构** 构建的现代化宠物领养与社区互动平台。系统融合了完整的领养流程管理、AI 智能审核与状态识别、多角色权限控制、异步消息通知以及用户社区生态，致力于打造安全、透明、温暖的宠物领养体验。 

---

## 🌟 核心功能亮点

- **全流程领养管理**：从浏览 → 申请 → 初审 → 复审 → 面谈 → 交接 → 领养后跟踪，覆盖领养全生命周期。
- **AI 智能辅助**：
  - 自动检测社区内容违规（涉黄、涉政、广告等）
  - 从“养宠日常”帖自动提取宠物健康状态并更新档案
  - 为“养宠攻略”生成智能摘要
- **多角色协同治理**：
  - 用户（申请者）
  - 机构管理员 & 宠物维护员
  - 平台审核员、客服人员、超级管理员
- **社区互动生态**：支持发帖、评论、点赞、举报、精选推荐，构建养宠知识与情感分享空间。
- **强一致性与高可用设计**：通过事件驱动 + RabbitMQ + 分布式事务保障数据最终一致性。

---

## 🏗️ 系统架构

### 微服务划分

| 服务名称 | 职责描述 |
|--------|--------|
| `auth-service` | 用户注册/登录、JWT 认证、RBAC 权限管理 |
| `org-service` | 机构入驻申请、成员管理、资质审核 |
| `pet-service` | 宠物信息 CRUD、健康记录、状态流转（可领养/已预约/已领养） |
| `adoption-service` | 领养申请主流程、状态机管理、材料审核 |
| `interview-service` | 面谈时段管理、预约确认、面谈结果记录 |
| `community-service` | 帖子/评论/点赞/举报、文件上传、AI 触发入口 |
| `ai-service` | 内容安全审核、宠物状态提取、攻略摘要生成 |
| `notification-service` | 统一消息中心，消费 RabbitMQ 事件并推送站内信/邮件 |
| `gateway` | 统一路由、JWT 鉴权、限流、Header 注入（用户ID/角色） |
| `eureka-server` | 服务注册与发现 |
| `config-server` | 集中化配置管理（支持多环境、敏感信息加密） |

---

## 🧩 技术栈

### 后端
- **语言**：Java 17
- **框架**：
  - Spring Boot 3.2.2
  - Spring Cloud 2023.0.0（Eureka, Config, Gateway, OpenFeign）
  - Spring Security + JWT
  - MyBatis
- **中间件**：
  - **消息队列**：RabbitMQ
  - **缓存**：Redis
  - **搜索**：Elasticsearch
  - **存储**：阿里云 OSS
- **数据库**：MySQL 8.0（每服务独立 schema，外键不跨库）
- **任务调度**：Spring Scheduler

### 前端
- React 18 + TypeScript
- 状态管理：Redux / Context
- UI 库：Ant Design / Tailwind CSS
- 文件上传：STS 临时凭证直传 OSS

---

## 🔄 核心业务流程

1. **游客浏览宠物** → 登录后申请领养  
2. **用户提交申请** → 机构初审 → 平台复审  
3. **面谈预约与确认** → 面谈结果录入  
4. **线下交接完成** → 宠物状态更新为“已领养”  
5. **领养后跟踪**：
   - 用户定期上传宠物反馈（图文/视频）
   - AI 自动解析内容更新宠物健康状态
   - 超期未更新 → 客服人工介入
6. **社区互动**：
   - 发帖触发 AI 审核 + 状态提取/摘要生成
   - 违规内容自动下架 → 客服复核
   - 超管推荐优质内容至首页轮播

---

## 📦 开发部署指南

### 前置依赖（Docker 推荐）
```bash
# 启动 MySQL
docker run -d --name mysql -p 3306:3306 -e MYSQL_ROOT_PASSWORD=root mysql:8.0

# 启动 Redis
docker run -d --name redis -p 6379:6379 redis

# 启动 RabbitMQ（带管理界面）
docker run -d --name rabbitmq -p 5672:5672 -p 15672:15672 rabbitmq:3-management
```

### 后端启动顺序
1. `eureka-server`（服务注册中心）
2. `config-server`（配置中心）
3. `auth-service`（认证服务）
4. 其他业务服务（`org`, `pet`, `adoption`, `community`, `ai`, `notification` 等）
5. `gateway`（API 网关）

> 所有服务通过 `application.yml` 从 Config Server 加载配置，数据库表结构在服务启动时自动初始化（`schema.sql`）。

### 前端启动
```bash
cd frontend
npm install
npm run dev
```

---

## 📬 消息通信机制

系统采用 **事件驱动架构（EDA）**，核心事件通过 RabbitMQ 异步传递：

- **Exchange**: `notify` (Topic 类型)
- **Routing Keys**: 
  - `notify.system`（系统通知）
  - `notify.direct`（私信）
  - `notify.likes`（点赞）
  - `adoption.*`, `org.*`, `ai.*` 等业务事件
- **消费者**: `notification-service` 统一处理并持久化通知

---

## 🔒 安全与权限

- **认证**：JWT Token（Bearer 认证）
- **授权**：基于角色的访问控制（RBAC），在 Gateway 层统一拦截
- **敏感操作**：关键接口幂等性设计（Redis 幂等键）
- **文件安全**：OSS 私有桶 + 临时签名 URL，前端直传避免服务端中转

---

## 📄 License

本项目采用 [Apache License 2.0](LICENSE) 开源协议。

---
