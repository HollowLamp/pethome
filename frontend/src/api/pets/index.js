import http from "../http";

// 宠物公共接口
export function fetchPets(params) {
  // params: { page?, pageSize?, type?, status?, orgId? }
  return http.get("/pets", { params });
}

export function getPetDetail(petId) {
  return http.get(`/pets/${petId}`);
}

// 愿望单相关
export function addToWishlist(petId) {
  return http.post(`/pets/${petId}/wishlist`);
}

export function removeFromWishlist(petId) {
  return http.delete(`/pets/${petId}/wishlist`);
}

// 机构宠物管理
export function createPet(payload) {
  return http.post("/pets/org", payload);
}

export function updatePet(petId, payload) {
  return http.patch(`/pets/org/${petId}`, payload);
}

export function updatePetStatus(petId, status) {
  return http.post(`/pets/org/${petId}/status`, { status });
}

export function uploadPetCover(petId, file) {
  const formData = new FormData();
  formData.append("file", file);
  return http.post(`/pets/org/${petId}/cover`, formData, {
    headers: {
      "Content-Type": "multipart/form-data",
    },
  });
}

// 健康记录
export function updatePetHealth(petId, payload) {
  return http.post(`/pets/${petId}/health`, payload);
}

export function getPetHealth(petId) {
  return http.get(`/pets/${petId}/health`);
}

export function getPetHealthHistory(petId) {
  return http.get(`/pets/${petId}/health/history`);
}

// 反馈
export function getPetFeedbacks(petId) {
  return http.get(`/pets/${petId}/feedbacks`);
}

export function createPetFeedback(petId, { content, files }) {
  const formData = new FormData();
  if (content) {
    formData.append("content", content);
  }
  if (Array.isArray(files)) {
    files.forEach((file) => {
      if (file) {
        formData.append("files", file);
      }
    });
  }

  return http.post(`/pets/${petId}/feedbacks`, formData, {
    headers: {
      "Content-Type": "multipart/form-data",
    },
  });
}

// 按宠物类型获取反馈（需登录）
export function getPetFeedbacksByType(type) {
  return http.get(`/pets/type/${type}/feedbacks`);
}

// 愿望单 - 获取当前用户的愿望单宠物列表
export function getWishlistPets() {
  return http.get(`/pets/wishlist`);
}

// C端用户上传健康状态
export function updatePetHealthByOwner(petId, payload) {
  return http.post(`/pets/${petId}/health/owner`, payload);
}

// B端：查询逾期未更新健康状态的宠物
export function getOverduePets(orgId, daysSinceUpdate = 30) {
  return http.get("/pets/health/overdue", {
    params: { orgId, daysSinceUpdate },
  });
}

// B端：发送逾期提醒通知
export function sendOverdueReminder(petId, applicantId, daysOverdue) {
  return http.post("/pets/health/remind-overdue", {
    petId,
    applicantId,
    daysOverdue,
  });
}
