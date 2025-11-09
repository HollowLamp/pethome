import http from "../http";

// ==================== 帖子相关 ====================

/**
 * 获取帖子列表
 * @param {Object} params - 查询参数
 * @param {string} params.type - 帖子类型（可选）
 * @param {string} params.sort - 排序方式（可选）
 * @param {number} params.page - 页码（可选）
 * @param {number} params.pageSize - 每页数量（可选）
 */
export function getPostList(params) {
  return http.get("/community/posts", { params });
}

/**
 * 获取帖子详情
 * @param {number} postId - 帖子ID
 */
export function getPostDetail(postId) {
  return http.get(`/community/posts/${postId}`);
}

/**
 * 发布帖子
 * @param {Object} payload - 帖子数据
 */
export function createPost(payload) {
  return http.post("/community/posts", payload);
}

/**
 * 删除帖子
 * @param {number} postId - 帖子ID
 */
export function deletePost(postId) {
  return http.delete(`/community/posts/${postId}`);
}

/**
 * 获取我发布的帖子
 * @param {Object} params - 查询参数
 * @param {number} params.page - 页码（可选）
 * @param {number} params.pageSize - 每页数量（可选）
 */
export function getMyPosts(params) {
  return http.get("/community/posts/my", { params });
}

/**
 * 上传单个文件（图片或视频）
 * @param {File} file - 文件对象
 */
export function uploadFile(file) {
  const formData = new FormData();
  formData.append("file", file);
  return http.post("/community/posts/upload", formData, {
    headers: {
      "Content-Type": "multipart/form-data",
    },
  });
}

/**
 * 批量上传文件（最多9个）
 * @param {File[]} files - 文件数组
 */
export function uploadFiles(files) {
  const formData = new FormData();
  files.forEach((file) => {
    if (file) {
      formData.append("files", file);
    }
  });
  return http.post("/community/posts/upload/batch", formData, {
    headers: {
      "Content-Type": "multipart/form-data",
    },
  });
}

// ==================== 评论相关 ====================

/**
 * 获取评论列表
 * @param {number} postId - 帖子ID
 * @param {Object} params - 查询参数
 * @param {number} params.page - 页码（可选）
 * @param {number} params.pageSize - 每页数量（可选）
 */
export function getComments(postId, params) {
  return http.get(`/community/posts/${postId}/comments`, { params });
}

/**
 * 发布评论
 * @param {number} postId - 帖子ID
 * @param {Object} payload - 评论数据
 */
export function createComment(postId, payload) {
  return http.post(`/community/posts/${postId}/comments`, payload);
}

/**
 * 删除评论
 * @param {number} commentId - 评论ID
 */
export function deleteComment(commentId) {
  return http.delete(`/community/comments/${commentId}`);
}

// ==================== 互动反应相关 ====================

/**
 * 点赞/取消点赞帖子（幂等操作）
 * @param {number} postId - 帖子ID
 */
export function togglePostLike(postId) {
  return http.post(`/community/posts/${postId}/like`);
}

/**
 * 点赞/取消点赞评论（幂等操作）
 * @param {number} commentId - 评论ID
 */
export function toggleCommentLike(commentId) {
  return http.post(`/community/comments/${commentId}/like`);
}

// ==================== 举报相关 ====================

/**
 * 举报帖子
 * @param {number} postId - 帖子ID
 * @param {Object} payload - 举报数据
 */
export function reportPost(postId, payload) {
  return http.post(`/community/posts/${postId}/report`, payload);
}

/**
 * 举报评论
 * @param {number} commentId - 评论ID
 * @param {Object} payload - 举报数据
 */
export function reportComment(commentId, payload) {
  return http.post(`/community/comments/${commentId}/report`, payload);
}

// ==================== 管理员相关 ====================

/**
 * 获取AI标记的违规帖子（客服）
 * @param {Object} params - 查询参数
 * @param {number} params.page - 页码（可选）
 * @param {number} params.pageSize - 每页数量（可选）
 */
export function getFlaggedPosts(params) {
  return http.get("/community/posts/flagged", { params });
}

/**
 * 修改帖子状态（客服）
 * @param {number} postId - 帖子ID
 * @param {string} status - 状态
 */
export function updatePostStatus(postId, status) {
  return http.patch(`/community/posts/${postId}/status`, { status });
}

/**
 * 获取举报列表（客服）
 * @param {Object} params - 查询参数
 * @param {string} params.status - 状态筛选（可选）
 * @param {number} params.page - 页码（可选）
 * @param {number} params.pageSize - 每页数量（可选）
 */
export function getReports(params) {
  return http.get("/community/reports", { params });
}

/**
 * 处理举报（客服）
 * @param {number} reportId - 举报ID
 * @param {string} status - 状态
 */
export function handleReport(reportId, status) {
  return http.patch(`/community/reports/${reportId}/status`, { status });
}

/**
 * 推荐/取消推荐帖子（超级管理员）
 * @param {number} postId - 帖子ID
 * @param {boolean} recommend - 是否推荐（可选，默认切换）
 */
export function toggleRecommend(postId, recommend) {
  const body = recommend !== undefined ? { recommend } : {};
  return http.post(`/community/posts/${postId}/recommend`, body);
}

