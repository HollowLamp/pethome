import http from "../http";

// 用户 - 提交面谈预约请求
export function requestInterview(appId, slotId) {
  // appId: 申请ID, slotId: 面谈时段ID
  return http.post(`/interview/adoptions/${appId}/interview/request`, {
    slotId,
  });
}

// 机构管理员 - 查看预约请求
export function getInterviewRequest(appId) {
  // appId: 申请ID
  return http.get(`/interview/adoptions/${appId}/interview`);
}

// 机构管理员 - 确认面谈
export function confirmInterview(appId) {
  // appId: 申请ID
  return http.post(`/interview/adoptions/${appId}/interview/confirm`);
}

// 机构管理员 - 完成面谈
export function completeInterview(appId) {
  // appId: 申请ID
  return http.post(`/interview/adoptions/${appId}/interview/complete`);
}

// 机构管理员 - 完成交接
export function completeHandover(appId) {
  // appId: 申请ID
  return http.post(`/interview/adoptions/${appId}/handover/complete`);
}

// 机构管理员 - 获取机构的时段列表
export function getScheduleSlots() {
  return http.get(`/interview/slots`);
}

// 机构管理员 - 创建时段
export function createScheduleSlot(slot) {
  // slot: { startAt, endAt, isOpen }
  return http.post(`/interview/slots`, slot);
}

// 机构管理员 - 更新时段
export function updateScheduleSlot(slotId, slot) {
  // slotId: 时段ID, slot: { startAt, endAt, isOpen }
  return http.put(`/interview/slots/${slotId}`, slot);
}

// 机构管理员 - 删除时段
export function deleteScheduleSlot(slotId) {
  // slotId: 时段ID
  return http.delete(`/interview/slots/${slotId}`);
}

// 用户 - 根据申请ID获取可用时段
export function getAvailableSlots(appId) {
  // appId: 申请ID
  return http.get(`/interview/adoptions/${appId}/slots`);
}
