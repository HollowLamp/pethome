# Org-Service Postman 测试接口清单

## 前置说明
- **网关地址**：`http://localhost:8080`
- **路由前缀**：`/org`
- **完整 URL**：`http://localhost:8080/org/...`
- **必需请求头**：`X-User-Id`（用于标识当前用户，由网关透传）

---

## 一、机构入驻申请流程

### 1. 机构入驻申请
**功能**：用户提交机构入驻申请，创建 PENDING 状态的机构记录

**请求信息**：
- **Method**：`POST`
- **URL**：`http://localhost:8080/org/apply`
- **Headers**：
  ```
  Content-Type: application/json
  X-User-Id: 1001
  ```
- **Body (JSON)**：
  ```json
  {
    "name": "可爱救助站",
    "licenseUrl": "http://example.com/license.jpg",
    "address": "上海市徐汇区XX路123号",
    "contactName": "张三",
    "contactPhone": "13800000000"
  }
  ```

**预期结果**：
- 返回成功消息："提交成功，等待审核"
- 数据库 `org` 表新增一条记录，`status = 'PENDING'`，`created_by = 1001`

---

### 2. 查询机构详情
**功能**：根据机构ID查询机构基本信息

**请求信息**：
- **Method**：`GET`
- **URL**：`http://localhost:8080/org/{id}`
  - 示例：`http://localhost:8080/org/1`（假设申请后返回的机构ID为1）
- **Headers**：
  ```
  Content-Type: application/json
  ```

**预期结果**：
- 返回机构完整信息（id, name, status, createdBy 等）

---

## 二、机构审核流程

### 3. 审核通过机构申请
**功能**：管理员审核通过机构入驻申请，将状态改为 APPROVED，并自动将创建者加入成员表

**请求信息**：
- **Method**：`POST`
- **URL**：`http://localhost:8080/org/{id}/approve`
  - 示例：`http://localhost:8080/org/1/approve`
- **Headers**：
  ```
  Content-Type: application/json
  ```
- **Body (JSON)**：
  ```json
  {
    "reason": "资质齐全，审核通过"
  }
  ```
  （reason 可选，可为空 `{}`）

**预期结果**：
- 返回成功消息："审核通过，机构创建者已成为 ORG_ADMIN"
- 数据库 `org.status = 'APPROVED'`
- 数据库 `org_member` 表新增一条记录（org_id=1, user_id=1001）

**注意事项**：
- 仅允许对 `PENDING` 状态的机构进行通过操作
- 若机构不存在或状态不对，返回 404/400

---

### 4. 审核拒绝机构申请
**功能**：管理员拒绝机构入驻申请，将状态改为 REJECTED

**请求信息**：
- **Method**：`POST`
- **URL**：`http://localhost:8080/org/{id}/reject`
  - 示例：`http://localhost:8080/org/1/reject`
- **Headers**：
  ```
  Content-Type: application/json
  ```
- **Body (JSON)**：
  ```json
  {
    "reason": "资质证明不清晰，请重新提交"
  }
  ```
  （reason 可选，可为空 `{}`）

**预期结果**：
- 返回成功消息："审核已拒绝"
- 数据库 `org.status = 'REJECTED'`

**注意事项**：
- 仅允许对 `PENDING` 状态的机构进行拒绝操作

---

## 三、机构成员管理流程

### 5. 添加机构成员
**功能**：机构拥有者将指定用户加入机构（成为机构成员）

**请求信息**：
- **Method**：`POST`
- **URL**：`http://localhost:8080/org/{id}/members`
  - 示例：`http://localhost:8080/org/1/members`
- **Headers**：
  ```
  Content-Type: application/json
  ```
- **Body (JSON)**：
  ```json
  {
    "userId": 2002
  }
  ```

**预期结果**：
- 返回成功消息："成员添加成功"
- 数据库 `org_member` 表新增一条记录（org_id=1, user_id=2002）

**注意事项**：
- 若成员已存在，返回 400："该用户已是机构成员"
- 若机构不存在，返回 404："机构不存在"

---

### 6. 查询机构成员列表
**功能**：查询指定机构的所有成员列表

**请求信息**：
- **Method**：`GET`
- **URL**：`http://localhost:8080/org/{id}/members`
  - 示例：`http://localhost:8080/org/1/members`
- **Headers**：
  ```
  Content-Type: application/json
  ```

**预期结果**：
- 返回成员列表数组，每个成员包含：id, orgId, userId, createdAt

---

### 7. 移除机构成员
**功能**：机构拥有者从机构中移除指定成员

**请求信息**：
- **Method**：`DELETE`
- **URL**：`http://localhost:8080/org/{id}/members/{uid}`
  - 示例：`http://localhost:8080/org/1/members/2002`
- **Headers**：
  ```
  Content-Type: application/json
  ```

**预期结果**：
- 返回成功消息："成员删除成功"
- 数据库 `org_member` 表删除对应记录

**注意事项**：
- 若成员不存在，返回 404："该成员不存在"

---

### 8. 查询用户的机构成员关系
**功能**：根据用户ID查询其加入的所有机构成员关系

**请求信息**：
- **Method**：`GET`
- **URL**：`http://localhost:8080/org/users/{uid}/memberships`
  - 示例：`http://localhost:8080/org/users/2002/memberships`
- **Headers**：
  ```
  Content-Type: application/json
  ```

**预期结果**：
- 返回数组，每个元素包含：id, orgId, userId, createdAt
- 若未加入任何机构，返回空数组

**使用示例**：
- 可与接口 #5、#6 联合验证：先新增成员，再根据 userId 查询该用户的所有机构

---

## 测试顺序建议

### 完整流程测试（推荐顺序）：
1. **申请入驻** → 使用接口 #1，记录返回的机构ID（假设为1）
2. **查询详情** → 使用接口 #2，确认状态为 PENDING
3. **审核通过** → 使用接口 #3，确认状态变为 APPROVED，成员表有创建者记录
4. **添加成员** → 使用接口 #5，添加 userId=2002
5. **查询成员列表** → 使用接口 #6，确认包含创建者(1001)和新成员(2002)
6. **移除成员** → 使用接口 #7，删除 userId=2002
7. **再次查询成员列表** → 确认只剩创建者

### 异常场景测试：
- 重复添加同一成员 → 应返回 400
- 对非 PENDING 状态机构进行审核 → 应返回 400
- 查询不存在的机构 → 应返回 404
- 删除不存在的成员 → 应返回 404

---

## Postman 环境变量建议

为方便测试，建议在 Postman 中设置环境变量：
- `base_url`: `http://localhost:8080`
- `org_id`: `1`（申请后动态更新）
- `user_id`: `1001`（申请者ID）
- `member_user_id`: `2002`（测试成员ID）

然后 URL 可写为：`{{base_url}}/org/{{org_id}}/members`

