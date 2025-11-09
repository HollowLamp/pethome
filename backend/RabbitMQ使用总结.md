# RabbitMQ 使用总结

## 概述
本文档总结了后端微服务架构中 RabbitMQ 消息队列的使用情况。

## RabbitMQ 配置

### 连接配置
在 `config-server` 的 `application.yml` 中配置：
```yaml
spring:
  rabbitmq:
    host: localhost
    port: 5672
    username: guest
    password: guest
```

### 依赖引入
所有使用 RabbitMQ 的服务都引入了以下依赖：
```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-amqp</artifactId>
</dependency>
```

---

## RabbitMQ 架构设计

### 消息流转架构

```
┌─────────────────┐
│  Producer       │  生产者服务（发送消息）
│  Services       │
│                 │
│  - adoption     │
│  - auth         │
│  - org          │
│  - pet          │
│  - community    │
└────────┬────────┘
         │
         │ send message
         ▼
┌─────────────────┐
│  Exchange       │  TopicExchange: "notify"
│  (notify)       │
└────────┬────────┘
         │
         │ routing by key
         ▼
┌─────────────────┐
│  Queue          │  "notify.queue"
│                 │
└────────┬────────┘
         │
         │ consume
         ▼
┌─────────────────┐
│  Consumer       │  消费者服务（接收消息）
│  notification   │
│  -service       │
└─────────────────┘
```

### 核心组件

#### 1. Exchange（交换机）
- **类型**：`TopicExchange`
- **名称**：`notify`
- **特点**：
  - 持久化（`durable = true`）
  - 不自动删除（`autoDelete = false`）
  - 支持通配符路由

#### 2. Queue（队列）
- **名称**：`notify.queue`
- **特点**：
  - 持久化队列
  - 仅在 `notification-service` 中定义
  - 其他服务只配置 Exchange，不配置 Queue

#### 3. Routing Key（路由键）
- **模式**：`notify.*`（通配符匹配）
- **具体路由键**：
  - `notify.system` - 系统通知
  - `notify.direct` - 私信通知
  - `notify.likes` - 点赞通知

#### 4. Binding（绑定）
- **规则**：`notify.queue` 绑定到 `notify` Exchange
- **路由键模式**：`notify.*`

---

## 服务使用情况

### 生产者服务（发送消息）

#### 1. adoption-service（领养服务）

**使用场景**：领养申请流程中的各种通知

**通知类型**：
1. **申请提交通知**
   - 发送给：申请人
   - 模板代码：`ADOPTION_SUBMITTED_TO_USER`
   - 场景：用户提交领养申请后

2. **新申请通知**
   - 发送给：机构管理员（ORG_ADMIN）
   - 模板代码：`ADOPTION_SUBMITTED`
   - 场景：机构收到新的领养申请

3. **申请通过初审通知**
   - 发送给：申请人
   - 模板代码：`ADOPTION_APPROVED_BY_ORG`
   - 场景：机构管理员通过初审

4. **准备复审通知**
   - 发送给：审核员（AUDITOR）
   - 模板代码：`ADOPTION_READY_FOR_REVIEW`
   - 场景：申请通过初审，等待平台复审

5. **申请被拒绝通知**
   - 发送给：申请人
   - 模板代码：`ADOPTION_REJECTED_BY_ORG`
   - 场景：机构管理员拒绝申请

6. **申请通过平台复审通知**
   - 发送给：申请人
   - 模板代码：`ADOPTION_APPROVED_BY_PLATFORM`
   - 场景：平台审核员通过复审

7. **平台审核通过通知**
   - 发送给：机构管理员
   - 模板代码：`ADOPTION_PLATFORM_APPROVED`
   - 场景：平台审核通过，通知机构准备交接

8. **申请被平台拒绝通知**
   - 发送给：申请人、机构管理员
   - 模板代码：`ADOPTION_REJECTED_BY_PLATFORM`
   - 场景：平台审核员拒绝申请

9. **交接完成通知**
   - 发送给：申请人、机构管理员、宠物信息维护员（ORG_STAFF）
   - 模板代码：`ADOPTION_HANDOVER_COMPLETED`
   - 场景：交接完成，领养流程结束

**特殊功能**：
- `sendSystemNotificationToRole()` - 批量发送给指定角色的所有用户
- `sendSystemNotificationToOrgAdmin()` - 发送给机构管理员

#### 2. auth-service（认证服务）

**使用场景**：用户注册欢迎通知

**通知类型**：
1. **欢迎消息**
   - 发送给：新注册用户
   - 模板代码：`USER_WELCOME`
   - 场景：用户注册成功后

#### 3. org-service（机构服务）

**使用场景**：机构管理流程中的通知

**通知类型**：
1. **新机构申请通知**
   - 发送给：审核员（AUDITOR）
   - 模板代码：`ORG_APPLIED`
   - 场景：机构提交入驻申请

2. **机构审核通过通知**
   - 发送给：机构创建者
   - 模板代码：`ORG_APPROVED`
   - 场景：审核员通过机构申请

3. **机构审核拒绝通知**
   - 发送给：机构创建者
   - 模板代码：`ORG_REJECTED`
   - 场景：审核员拒绝机构申请

4. **成员添加通知**
   - 发送给：被添加的成员
   - 场景：机构管理员添加新成员

5. **成员删除通知**
   - 发送给：被删除的成员
   - 场景：机构管理员删除成员

#### 4. pet-service（宠物服务）

**使用场景**：宠物健康记录提醒

**通知类型**：
1. **健康状态更新提醒**
   - 发送给：宠物主人（领养人）
   - 模板代码：`HEALTH_UPDATE_OVERDUE`
   - 场景：宠物健康状态超过指定天数未更新

#### 5. community-service（社区服务）

**使用场景**：社区互动通知

**通知类型**：
1. **评论通知**
   - 发送给：帖子作者
   - 场景：有人评论了用户的帖子

2. **举报通知**
   - 发送给：客服人员（CS）
   - 场景：用户提交举报

3. **举报处理通知**
   - 发送给：举报人
   - 场景：举报处理完成

4. **点赞通知**（预留）
   - 发送给：帖子/评论作者
   - 场景：有人点赞了用户的内容

### 消费者服务（接收消息）

#### notification-service（通知服务）

**职责**：
- 接收所有 RabbitMQ 消息
- 处理不同类型的通知事件
- 创建通知任务和收件箱消息
- 存储到数据库

**监听器**：`NotificationEventListener`
- 监听队列：`notify.queue`
- 处理事件类型：
  - `notify.system` - 系统通知
  - `notify.direct` - 私信通知
  - `notify.likes` - 点赞通知

---

## 消息格式

### 标准消息格式

```json
{
  "eventType": "notify.system",  // 事件类型
  "payload": {                    // 消息负载
    "userId": 123,                // 接收者用户ID（必需）
    "title": "通知标题",           // 通知标题（必需）
    "body": "通知内容",            // 通知内容（必需）
    "templateCode": "TEMPLATE_CODE", // 模板代码（可选）
    "channel": "SYSTEM"           // 通知渠道（可选，默认 SYSTEM）
  }
}
```

### 不同事件类型的 Payload

#### 1. 系统通知（notify.system）
```json
{
  "eventType": "notify.system",
  "payload": {
    "userId": 123,
    "title": "申请已通过",
    "body": "您的领养申请已通过审核",
    "templateCode": "ADOPTION_APPROVED"
  }
}
```

#### 2. 私信通知（notify.direct）
```json
{
  "eventType": "notify.direct",
  "payload": {
    "fromUserId": 456,
    "toUserId": 123,
    "content": "私信内容"
  }
}
```

#### 3. 点赞通知（notify.likes）
```json
{
  "eventType": "notify.likes",
  "payload": {
    "userId": 123,
    "title": "点赞通知",
    "body": "有人给你点赞了"
  }
}
```

---

## 使用统计

### 按服务统计（生产者）

| 服务 | 使用场景数 | 主要通知类型 |
|------|----------|-------------|
| adoption-service | 9+ | 领养申请流程通知 |
| org-service | 5 | 机构管理流程通知 |
| community-service | 3+ | 社区互动通知 |
| auth-service | 1 | 用户注册欢迎 |
| pet-service | 1 | 健康记录提醒 |

### 按通知类型统计

| 通知类型 | 使用服务数 | 主要场景 |
|---------|----------|---------|
| 系统通知（notify.system） | 5 | 各种业务状态变更通知 |
| 私信通知（notify.direct） | 1 | 社区私信功能 |
| 点赞通知（notify.likes） | 1 | 社区点赞功能 |

### 按角色统计（接收者）

| 角色 | 接收通知场景 |
|------|-------------|
| USER | 申请状态变更、欢迎消息、健康提醒等 |
| ORG_ADMIN | 新申请、审核结果、交接完成等 |
| AUDITOR | 新机构申请、准备复审等 |
| CS | 举报处理 |
| ORG_STAFF | 交接完成 |

---

## 实现模式

### 1. 统一配置模式

每个服务都创建了 `RabbitMQConfig` 配置类：
- 配置相同的 Exchange（`notify`）
- 配置 JSON 消息转换器
- 配置 RabbitTemplate

### 2. 封装服务模式

每个服务都创建了 `NotificationMessageService`：
- 封装消息发送逻辑
- 提供统一的接口
- 处理异常，不影响主业务流程

### 3. 消息格式统一

所有服务使用相同的消息格式：
- `eventType` + `payload` 结构
- JSON 序列化
- 统一的字段命名

### 4. 错误处理模式

```java
try {
    notificationMessageService.sendSystemNotification(...);
} catch (Exception e) {
    // 消息发送失败不影响主业务流程，只记录日志
    log.warn("发送通知失败: {}", e.getMessage());
}
```

---

## 特殊功能

### 1. 批量发送给角色

**实现服务**：`adoption-service`、`org-service`、`community-service`

**功能**：`sendSystemNotificationToRole(role, title, body, templateCode)`

**实现方式**：
1. 通过 Feign 调用 `auth-service` 获取用户列表
2. 分页查询所有用户
3. 筛选出拥有指定角色的用户
4. 批量发送通知给每个用户

**使用场景**：
- 发送给所有审核员（AUDITOR）
- 发送给所有机构管理员（ORG_ADMIN）
- 发送给所有客服人员（CS）

### 2. 发送给机构管理员

**实现服务**：`adoption-service`

**功能**：`sendSystemNotificationToOrgAdmin(orgId, title, body, templateCode)`

**实现方式**：
1. 获取所有机构管理员（ORG_ADMIN 角色）
2. 发送通知（TODO: 应该根据 orgId 过滤）

**优化建议**：应该通过 `org-service` 获取该机构的实际管理员列表

---

## 使用流程示例

### 示例 1：用户提交领养申请

```
1. 用户提交申请
   ↓
2. adoption-service 保存申请到数据库
   ↓
3. adoption-service 发送通知给申请人
   rabbitTemplate.convertAndSend("notify", "notify.system", messageJson)
   ↓
4. notification-service 接收消息
   @RabbitListener(queues = "notify.queue")
   ↓
5. notification-service 处理消息
   - 创建通知任务
   - 创建收件箱消息
   - 存储到数据库
   ↓
6. 用户在前端查看通知
```

### 示例 2：机构审核通过

```
1. 审核员审核通过机构申请
   ↓
2. org-service 更新机构状态
   ↓
3. org-service 发送通知给机构创建者
   notificationMessageService.sendSystemNotification(...)
   ↓
4. RabbitMQ 消息流转
   Exchange → Queue → Consumer
   ↓
5. notification-service 处理并存储
   ↓
6. 机构创建者收到通知
```

---

## 注意事项

### 1. Exchange 名称一致性
- ✅ 所有服务使用相同的 Exchange 名称：`"notify"`
- ✅ RabbitMQ 会自动复用同名 Exchange

### 2. Routing Key 规范
- ✅ 使用统一的路由键格式：`notify.{type}`
- ✅ 必须与队列的 Binding 规则匹配（`notify.*`）

### 3. 消息格式规范
- ✅ 所有服务使用相同的消息格式
- ✅ 必需字段：`userId`、`title`、`body`
- ✅ 可选字段：`templateCode`、`channel`

### 4. 错误处理
- ✅ 消息发送失败不影响主业务流程
- ✅ 使用 try-catch 捕获异常
- ✅ 记录日志便于排查问题

### 5. 性能考虑
- ✅ 异步发送消息，不阻塞主流程
- ⚠️ 批量发送时注意性能（大量用户）
- ⚠️ 考虑消息积压情况

### 6. 消息可靠性
- ✅ Exchange 和 Queue 都设置为持久化
- ⚠️ 消息本身未设置持久化（可能需要配置）
- ⚠️ 未实现消息确认机制（ack）

---

## 优化建议

### 1. 消息持久化
建议配置消息持久化，确保消息不丢失：
```java
MessageProperties properties = new MessageProperties();
properties.setDeliveryMode(MessageDeliveryMode.PERSISTENT);
```

### 2. 消息确认机制
实现消息确认机制，确保消息被正确处理：
- 生产者确认（Publisher Confirms）
- 消费者确认（Consumer Acknowledgments）

### 3. 死信队列
配置死信队列，处理无法处理的消息：
- 消息被拒绝且不重新入队
- 消息过期
- 队列达到最大长度

### 4. 消息重试机制
实现消息重试机制，处理临时失败：
- 使用 `@Retryable` 注解
- 配置重试次数和间隔

### 5. 监控和告警
添加监控和告警：
- 消息积压监控
- 消费速度监控
- 错误率监控

### 6. 代码复用
考虑将 `NotificationMessageService` 提取到 `common` 模块：
- 减少代码重复
- 统一消息格式和处理逻辑
- 便于维护和升级

---

## 总结

### 当前状态
- **使用范围**：5 个服务作为生产者，1 个服务作为消费者
- **消息类型**：3 种（系统通知、私信、点赞）
- **使用场景**：20+ 个业务场景
- **架构模式**：发布-订阅模式（Topic Exchange）

### 特点
1. **统一架构**：所有服务使用相同的 Exchange 和消息格式
2. **解耦设计**：通过消息队列实现服务间异步通信
3. **错误隔离**：消息发送失败不影响主业务流程
4. **扩展性强**：易于添加新的通知类型和场景

### 优势
1. **异步处理**：提高系统响应速度
2. **服务解耦**：减少服务间直接依赖
3. **可靠性**：消息持久化保证不丢失
4. **可扩展**：易于添加新的生产者和消费者

### 改进方向
1. 完善消息持久化和确认机制
2. 实现死信队列和重试机制
3. 添加监控和告警
4. 代码复用和统一管理

