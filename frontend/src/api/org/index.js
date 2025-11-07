import http from "../http";

// 机构入驻申请
export function applyOrg(payload) {
  return http.post("/org/apply", payload);
}

// 审核通过
export function approveOrg(orgId, payload) {
  return http.post(`/org/${orgId}/approve`, payload);
}

// 审核拒绝
export function rejectOrg(orgId, payload) {
  return http.post(`/org/${orgId}/reject`, payload);
}

// 查询机构详情
export function getOrgDetail(orgId) {
  return http.get(`/org/${orgId}`);
}

// 添加机构成员
export function addOrgMember(orgId, payload) {
  return http.post(`/org/${orgId}/members`, payload);
}

// 移除机构成员
export function removeOrgMember(orgId, userId) {
  return http.delete(`/org/${orgId}/members/${userId}`);
}

// 获取机构成员列表
export function getOrgMembers(orgId) {
  return http.get(`/org/${orgId}/members`);
}

// 查询用户加入的机构列表
export function getUserMemberships(userId) {
  return http.get(`/org/users/${userId}/memberships`);
}

// 上传机构资质文件
export function uploadOrgLicense(file, orgId) {
  const formData = new FormData();
  formData.append("file", file);
  if (orgId) {
    formData.append("orgId", orgId);
  }
  return http.post("/org/license/upload", formData, {
    headers: {
      "Content-Type": "multipart/form-data",
    },
  });
}

// 获取待审核的机构列表
export function getPendingOrganizations() {
  return http.get("/org/applications/pending");
}
