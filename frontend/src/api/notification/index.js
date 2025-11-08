import http from "../http";

// C端用户消息接口
// 获取系统通知
export function getSystemMessages() {
  return http.get("/notification/me/messages/system");
}

// 获取私信消息
export function getDirectMessages() {
  return http.get("/notification/me/messages/direct");
}

// 发送私信
export function sendDirectMessage(toUserId, content) {
  return http.post("/notification/me/messages/direct", {
    toUserId,
    content,
  });
}

// 获取点赞通知
export function getLikeMessages() {
  return http.get("/notification/me/messages/likes");
}

// B端用户消息接口
// 获取系统通知
export function getOrgSystemMessages() {
  return http.get("/notification/org/messages/system");
}

// 标记单条消息为已读
export function markMessageAsRead(messageId) {
  return http.put(`/notification/me/messages/${messageId}/read`);
}

// 标记所有消息为已读
export function markAllMessagesAsRead() {
  return http.put("/notification/me/messages/read-all");
}

// B端：标记单条消息为已读
export function markOrgMessageAsRead(messageId) {
  return http.put(`/notification/org/messages/${messageId}/read`);
}

// B端：标记所有消息为已读
export function markAllOrgMessagesAsRead() {
  return http.put("/notification/org/messages/read-all");
}

// 获取未读消息数量（从收件箱消息中统计）
export function getUnreadCount() {
  // 这里可以调用一个专门的接口，或者前端自己统计
  // 暂时通过获取所有消息来统计未读数
  return http.get("/notification/me/messages/system").then((res) => {
    // 这里需要根据实际返回的数据结构来统计
    // 假设返回的是 { code: 200, data: [...] }
    const messages = res.data || [];
    // 统计未读数量（需要根据实际数据结构调整）
    return { code: 200, data: { count: 0 } };
  });
}

