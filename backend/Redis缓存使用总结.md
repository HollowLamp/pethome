# Redis 缓存使用总结

## 概述
本文档总结了后端微服务架构中 Redis 缓存的使用情况。

## Redis 配置

### 连接配置
在 `config-server` 的 `application.yml` 中配置：
```yaml
spring:
  redis:
    host: localhost
    port: 6379
```

### 依赖引入
以下服务引入了 Redis 依赖（`spring-boot-starter-data-redis`）：
- `auth-service` - 实际使用 Redis
- `ai-service` - 已引入但未使用
- `interview-service` - 已引入但未使用
- 其他服务未引入 Redis 依赖

---

## Redis 使用情况

### 1. auth-service（认证服务）

**使用场景：注册验证码缓存**

#### 实现类
- **类名**：`AuthService`
- **Redis 模板**：`StringRedisTemplate`

#### 具体使用

##### 1.1 存储验证码
```java
// 发送注册验证码时，将验证码存储到 Redis
String key = buildRegisterCodeKey(email);
String code = String.valueOf((int)(Math.random() * 900000) + 100000);
// 缓存 5 分钟
stringRedisTemplate.opsForValue().set(key, code, Duration.ofMinutes(5));
```

**键名格式**：`auth:register:code:{email}`
- 示例：`auth:register:code:user@example.com`

**过期时间**：5 分钟

**操作类型**：`String` 类型操作（`opsForValue()`）

##### 1.2 验证验证码
```java
// 注册时验证验证码
String key = buildRegisterCodeKey(dto.getEmail());
String cachedCode = stringRedisTemplate.opsForValue().get(key);
if (cachedCode == null || !cachedCode.equals(dto.getCode())) {
    return ApiResponse.error(4002, "验证码错误或已过期");
}
```

##### 1.3 删除验证码
```java
// 注册成功后删除验证码
stringRedisTemplate.delete(key);
```

#### 使用流程
1. 用户请求发送注册验证码 → `sendRegisterCode(email)`
2. 生成 6 位随机验证码
3. 将验证码存储到 Redis，键名为 `auth:register:code:{email}`，过期时间 5 分钟
4. 发送邮件给用户
5. 用户提交注册信息（包含验证码）
6. 从 Redis 读取验证码进行验证
7. 验证成功后删除 Redis 中的验证码

---

## Redis 键名规范

### 常量定义
在 `common` 模块中定义了 Redis 键名常量（`RedisKeys.java`）：
```java
public class RedisKeys {
    public static final String USER_SESSION = "user:session:";
    public static final String PET_CACHE = "pet:info:";
    public static final String ADOPTION_FLOW = "adoption:flow:";
}
```

**注意**：这些常量目前**未在代码中实际使用**，可能是预留的键名规范。

### 实际使用的键名
- `auth:register:code:{email}` - 注册验证码

### 键名命名规范
- 使用冒号（`:`）分隔层级
- 格式：`{服务名}:{业务}:{类型}:{标识符}`
- 示例：`auth:register:code:user@example.com`

---

## Redis 数据结构使用

### 当前使用的数据结构
- **String**：用于存储验证码
  - 操作：`opsForValue().set()`、`opsForValue().get()`、`delete()`

### 预留的数据结构（未使用）
根据 `RedisKeys` 常量，可能计划使用：
- **String**：用户会话（`user:session:`）
- **String**：宠物信息缓存（`pet:info:`）
- **String**：领养流程缓存（`adoption:flow:`）

---

## Redis 使用统计

### 按服务统计

| 服务 | 是否使用 Redis | 使用场景 | 数据结构 |
|------|---------------|---------|---------|
| auth-service | ✅ 是 | 注册验证码缓存 | String |
| ai-service | ❌ 否 | 已引入依赖但未使用 | - |
| interview-service | ❌ 否 | 已引入依赖但未使用 | - |
| adoption-service | ❌ 否 | 未引入依赖 | - |
| pet-service | ❌ 否 | 未引入依赖 | - |
| org-service | ❌ 否 | 未引入依赖 | - |
| community-service | ❌ 否 | 未引入依赖 | - |
| notification-service | ❌ 否 | 未引入依赖 | - |

### 使用场景统计

| 使用场景 | 服务 | 键名格式 | 过期时间 | 操作类型 |
|---------|------|---------|---------|---------|
| 注册验证码 | auth-service | `auth:register:code:{email}` | 5 分钟 | String |

---

## Redis 使用模式

### 1. 临时数据缓存模式
**场景**：注册验证码
- **特点**：数据有时效性，使用后即删除
- **实现**：使用 `set(key, value, duration)` 设置过期时间
- **清理**：验证成功后立即删除

### 2. 键名构建模式
```java
private String buildRegisterCodeKey(String email) {
    return "auth:register:code:" + email;
}
```
- 使用私有方法统一构建键名
- 保证键名格式一致性

---

## 潜在优化建议

### 1. 会话管理
可以使用 Redis 存储用户会话信息：
- 键名：`user:session:{userId}`
- 存储 JWT token 或会话信息
- 支持分布式会话管理

### 2. 数据缓存
可以使用 Redis 缓存热点数据：
- **宠物信息缓存**：`pet:info:{petId}`
  - 缓存宠物详情，减少数据库查询
  - 设置合理的过期时间（如 1 小时）
- **机构信息缓存**：`org:info:{orgId}`
  - 缓存机构详情
  - 机构信息变更时清除缓存

### 3. 分布式锁
可以使用 Redis 实现分布式锁：
- 防止并发操作导致的数据不一致
- 例如：防止重复提交领养申请

### 4. 计数器
可以使用 Redis 实现计数器：
- 点赞数、评论数等实时统计
- 使用 `INCR`、`DECR` 操作

### 5. 限流
可以使用 Redis 实现接口限流：
- 防止恶意请求
- 使用滑动窗口或令牌桶算法

---

## 注意事项

### 1. 键名管理
- ✅ 使用常量类统一管理键名（`RedisKeys`）
- ⚠️ 当前常量未实际使用，建议统一使用常量类

### 2. 过期时间
- ✅ 验证码设置了合理的过期时间（5 分钟）
- ⚠️ 其他缓存场景需要根据业务需求设置过期时间

### 3. 异常处理
- ✅ 验证码验证时检查了 Redis 返回值的空值情况
- ⚠️ 建议添加 Redis 连接异常处理

### 4. 数据一致性
- ✅ 验证码使用后立即删除，避免重复使用
- ⚠️ 其他缓存场景需要考虑数据更新时的缓存失效策略

### 5. 性能考虑
- ✅ 使用 `StringRedisTemplate` 进行字符串操作，性能较好
- ⚠️ 大量数据缓存时需要考虑内存使用

---

## 总结

### 当前状态
- **使用范围**：仅在 `auth-service` 中使用 Redis
- **使用场景**：注册验证码缓存
- **数据结构**：String
- **使用模式**：临时数据缓存

### 特点
1. **轻量级使用**：仅用于验证码缓存，使用简单
2. **时效性管理**：设置了合理的过期时间
3. **键名规范**：有常量类定义，但未完全使用

### 扩展方向
1. 扩展缓存场景（宠物信息、机构信息等）
2. 实现分布式会话管理
3. 实现分布式锁和限流功能
4. 统一键名管理规范

