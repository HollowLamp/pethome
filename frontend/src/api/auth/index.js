import http from "../http";

// Auth 模块 API
// 后端参考：/auth/login, /auth/register, /auth/roles/assign, /auth/users/{id}/roles

export function loginApi(payload) {
  // payload: { username, password }
  return http.post("/auth/login", payload); // 返回 { code, data, message }
}

export function registerApi(payload) {
  // payload: { username, password, email, phone, code }
  return http.post("/auth/register", payload);
}

export function sendRegisterCodeApi(email) {
  // email: string
  return http.post("/auth/register/code", { email });
}

export function assignRoleApi(payload) {
  // payload: { userId, role }
  return http.post("/auth/roles/assign", payload);
}

export function getUserRolesApi(userId) {
  return http.get(`/auth/users/${userId}/roles`);
}

export function getUserListApi(params) {
  // params: { page, pageSize }
  return http.get("/auth/users", { params });
}

export function removeRoleApi(payload) {
  // payload: { userId, role }
  return http.delete("/auth/roles/remove", { data: payload });
}
