# AI 功能实现说明

## 概述

本文档说明了 AI 功能的实现方案，包括数据库设计、服务架构和集成方式。

## 功能需求

1. **发养宠日常（DAILY）**：触发 AI 分析内容，尝试自动更新宠物健康状态
2. **发养宠攻略（GUIDE）**：触发 AI 分析内容，自行填充帖子的 AI 总结字段
3. **发任意帖子**：触发 AI 分析内容，进行违规检测，在 AI 标记字段标记

## 数据库设计

### ai_task 表

用于记录所有 AI 分析任务。

```sql
CREATE TABLE IF NOT EXISTS ai_task (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    type ENUM('STATE_EXTRACT', 'CONTENT_MOD', 'STALE_CHECK', 'SUMMARY') NOT NULL,
    source_id BIGINT NOT NULL,
    source_type ENUM('POST', 'PET') NOT NULL DEFAULT 'POST',
    status ENUM('PENDING', 'DONE', 'FAILED') NOT NULL DEFAULT 'PENDING',
    result_json JSON,
    confidence DECIMAL(3,2),
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_source (source_type, source_id),
    INDEX idx_status (status),
    INDEX idx_type (type),
    INDEX idx_created_at (created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='AI任务表';
```

**字段说明**：
- `type`: 任务类型
  - `STATE_EXTRACT`: 状态提取（从养宠日常中提取健康状态）
  - `CONTENT_MOD`: 内容审核（违规检测）
  - `STALE_CHECK`: 过期检查（暂未使用）
  - `SUMMARY`: 内容总结（生成摘要）
- `source_id`: 来源ID（如帖子ID）
- `source_type`: 来源类型（POST-帖子，PET-宠物）
- `status`: 任务状态（PENDING-待处理，DONE-已完成，FAILED-失败）
- `result_json`: 分析结果（JSON格式）
- `confidence`: 置信度（0.00-1.00）

### 关于 pet_state_timeline 表

根据需求分析，**pet_state_timeline 表暂不需要**，原因：
1. 宠物健康状态已经在 `pet-service` 中管理（`pet_health` 表）
2. AI 提取的状态可以直接通过 Feign 调用 `pet-service` 更新
3. 避免数据冗余和同步问题

如果未来需要记录 AI 提取的状态历史，可以考虑：
- 在 `pet-service` 的 `pet_health` 表中添加 `source_type` 字段（MANUAL/AI）
- 或者通过 RabbitMQ 异步更新，由 `pet-service` 自行记录

## 服务架构

### ai-service（AI 服务）

**职责**：
- 提供 AI 分析接口
- 记录 AI 任务
- 返回分析结果

**主要接口**：
1. `POST /ai/content-moderation` - 内容审核（违规检测）
2. `POST /ai/summary` - 生成内容总结
3. `POST /ai/state-extract` - 提取宠物健康状态

**实现状态**：
- ✅ 基础架构已实现
- ✅ 接口已定义
- ⚠️ **TODO**: 调用真实 AI 接口（目前使用模拟数据）

### community-service（社区服务）

**集成方式**：
- 通过 Feign 客户端调用 `ai-service`
- 在 `PostService.createPost()` 中异步触发 AI 分析

**触发逻辑**：
1. 所有帖子：违规检测（`CONTENT_MOD`）
2. 养宠日常（`DAILY`）：状态提取（`STATE_EXTRACT`）
3. 养宠攻略（`GUIDE`）：内容总结（`SUMMARY`）

## 实现细节

### 1. AI 服务实现

**文件结构**：
```
ai-service/
├── src/main/java/com/adoption/ai/
│   ├── AiServiceApplication.java      # 启动类
│   ├── model/
│   │   └── AiTask.java                # AI任务实体
│   ├── repository/
│   │   └── AiTaskMapper.java          # 数据访问层
│   ├── service/
│   │   └── AiService.java             # AI服务逻辑
│   └── controller/
│       └── AiController.java         # REST控制器
└── src/main/resources/
    └── schema.sql                     # 数据库表结构
```

**关键代码**：
- `AiService.checkContentModeration()` - 内容审核（TODO: 调用真实 AI）
- `AiService.generateSummary()` - 生成总结（TODO: 调用真实 AI）
- `AiService.extractPetState()` - 提取状态（TODO: 调用真实 AI）

### 2. 社区服务集成

**文件修改**：
- `PostMapper.java` - 添加 `updateAiSummary()` 和 `updateAiFlagged()` 方法
- `PostService.java` - 添加 `triggerAiAnalysis()` 方法
- `AiServiceClient.java` - Feign 客户端接口

**触发流程**：
```
用户发布帖子
  ↓
PostService.createPost()
  ↓
保存帖子到数据库
  ↓
异步触发 triggerAiAnalysis()
  ↓
根据帖子类型调用不同的 AI 接口
  ↓
更新帖子字段（aiSummary、aiFlagged）
```

### 3. 错误处理

- AI 分析失败不影响帖子发布
- 使用 try-catch 捕获异常
- 记录日志便于排查问题

## TODO 事项

### 1. 调用真实 AI 接口

在 `AiService` 中的以下方法需要替换为真实 AI 调用：

```java
// TODO: 调用真实的 AI 接口进行内容审核
private boolean simulateContentModeration(String title, String content) {
    // 替换为真实的 AI API 调用
}

// TODO: 调用真实的 AI 接口生成内容总结
private String simulateSummary(String title, String content) {
    // 替换为真实的 AI API 调用
}

// TODO: 调用真实的 AI 接口提取宠物健康状态
private Map<String, Object> simulateStateExtraction(String content) {
    // 替换为真实的 AI API 调用
}
```

**建议**：
- 使用 OpenAI API、百度文心一言、阿里通义千问等
- 或者使用开源的 LLM 模型（如 Llama、ChatGLM）
- 封装统一的 AI 调用工具类

### 2. 自动更新宠物健康状态

在 `PostService.triggerAiAnalysis()` 中，养宠日常的状态提取完成后：

```java
// TODO: 根据提取的状态，尝试自动更新宠物健康状态
// 需要：
// 1. 从帖子中获取绑定的宠物ID（如果有 bindPetId）
// 2. 通过 Feign 调用 pet-service 更新健康状态
// 3. 或者通过 RabbitMQ 发送消息给 pet-service
// 注意：需要验证用户是否领养了该宠物
```

**实现方案**：
1. 检查帖子是否有 `bindPetId`
2. 通过 Feign 调用 `adoption-service` 验证用户是否领养了该宠物
3. 如果验证通过，通过 Feign 调用 `pet-service` 更新健康状态
4. 或者通过 RabbitMQ 异步处理，避免阻塞

### 3. 异步处理优化

当前实现是同步调用 AI 服务，建议优化为：
- 使用 `@Async` 注解异步处理
- 或者通过 RabbitMQ 消息队列异步处理
- 提高用户体验，不阻塞帖子发布

## 使用示例

### 1. 发布养宠日常

```json
POST /community/posts
{
  "type": "DAILY",
  "title": "今天带狗狗去公园",
  "content": "今天天气很好，带狗狗去公园玩，它很开心，食欲也很好...",
  "bindPetId": 123
}
```

**AI 处理流程**：
1. 违规检测 → 更新 `aiFlagged`
2. 状态提取 → 提取健康状态（TODO: 更新宠物健康状态）

### 2. 发布养宠攻略

```json
POST /community/posts
{
  "type": "GUIDE",
  "title": "如何训练狗狗上厕所",
  "content": "训练狗狗上厕所需要耐心和技巧..."
}
```

**AI 处理流程**：
1. 违规检测 → 更新 `aiFlagged`
2. 内容总结 → 更新 `aiSummary`

### 3. 发布任意帖子

```json
POST /community/posts
{
  "type": "PET_PUBLISH",
  "title": "可爱的小猫待领养",
  "content": "这是一只非常可爱的小猫..."
}
```

**AI 处理流程**：
1. 违规检测 → 更新 `aiFlagged`

## 注意事项

1. **AI 服务可用性**：如果 AI 服务不可用，帖子仍能正常发布
2. **性能考虑**：AI 分析可能耗时较长，建议异步处理
3. **数据一致性**：AI 分析结果更新失败时，不影响帖子数据
4. **成本控制**：真实 AI 调用可能产生费用，需要控制调用频率
5. **结果准确性**：AI 分析结果仅供参考，重要决策需要人工审核

## 后续优化

1. **缓存机制**：对相似内容的分析结果进行缓存
2. **批量处理**：对大量帖子进行批量 AI 分析
3. **结果反馈**：收集人工审核结果，用于优化 AI 模型
4. **监控告警**：监控 AI 服务调用情况，异常时告警
5. **降级策略**：AI 服务不可用时，使用规则引擎作为降级方案

