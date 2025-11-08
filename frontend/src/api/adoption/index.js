import http from "../http";

// 用户 - 提交领养申请
export function submitAdoption(payload) {
  // payload: AdoptionApp 对象
  return http.post("/adoptions", payload);
}

// 用户 - 查看我的申请
export function getMyApplications() {
  return http.get("/adoptions/me/adoptions");
}

// 用户 - 查看申请详情
export function getApplicationDetail(id) {
  return http.get(`/adoptions/${id}`);
}

// 获取申请材料列表
export function getApplicationDocs(id) {
  return http.get(`/adoptions/${id}/docs`);
}

// 用户 - 查看已领养宠物
export function getAdoptedPets() {
  return http.get("/adoptions/me/adoptions/adopted");
}

// 机构管理员 - 查看待审核申请
export function getPendingApplications(status) {
  // status: 可选，申请状态筛选
  const params = status ? { status } : {};
  return http.get("/adoptions/org/adoptions", { params });
}

// 机构管理员 - 初审申请通过
export function approveApplication(id) {
  return http.post(`/adoptions/${id}/approve`);
}

// 机构管理员 - 初审申请拒绝
export function rejectApplication(id, rejectReason) {
  // rejectReason: 拒绝原因（字符串）
  return http.post(`/adoptions/${id}/reject`, rejectReason);
}

// 审核员 - 复审申请批准
export function platformApproveApplication(id) {
  return http.post(`/adoptions/${id}/platform-approve`);
}

// 审核员 - 复审申请拒绝
export function platformRejectApplication(id, rejectReason) {
  // rejectReason: 拒绝原因（字符串）
  return http.post(`/adoptions/${id}/platform-reject`, rejectReason);
}

// 机构管理员 - 确认交接完成
export function completeHandover(id) {
  return http.post(`/adoptions/${id}/handover/complete`);
}

// 用户 - 上传领养资料材料
export function uploadProfileDoc(file, docType) {
  // file: File对象, docType: 材料类型（ID_CARD, INCOME_PROOF, PET_HISTORY 等）
  const formData = new FormData();
  formData.append("file", file);
  formData.append("docType", docType);
  return http.post("/adoptions/me/profile/upload", formData, {
    headers: {
      "Content-Type": "multipart/form-data",
    },
  });
}

// 用户 - 获取领养资料
export function getUserProfile() {
  return http.get("/adoptions/me/profile");
}

// 用户 - 删除领养资料中的某个材料
export function deleteProfileDoc(docId) {
  return http.delete(`/adoptions/me/profile/${docId}`);
}

// 用户 - 检查是否填写了领养资料
export function checkUserProfile() {
  return http.get("/adoptions/me/profile/check");
}
