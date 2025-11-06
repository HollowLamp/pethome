import axios from "axios";

const http = axios.create({
  baseURL: import.meta.env.VITE_API_BASE_URL || "http://localhost:8080",
  timeout: 15000,
  headers: {
    "Content-Type": "application/json",
  },
});

// 请求拦截：自动附带 Bearer Token
http.interceptors.request.use(
  (config) => {
    const token = localStorage.getItem("token");
    if (token) {
      config.headers = config.headers || {};
      config.headers.Authorization = `Bearer ${token}`;
    }
    return config;
  },
  (error) => Promise.reject(error)
);

// 响应拦截：统一处理后端 ApiResponse 结构 { code, data, message }
http.interceptors.response.use(
  (response) => {
    const { data } = response;
    // 约定成功 code 为 200（可根据后端调整）
    if (data && typeof data === "object" && "code" in data) {
      if (data.code === 200) return data;
      return Promise.reject({
        code: data.code,
        message: data.message || "请求失败",
      });
    }
    // 非约定结构，直接返回
    return data;
  },
  (error) => {
    if (error?.response?.status === 401) {
      // 未授权，清理本地状态
      localStorage.removeItem("token");
    }
    return Promise.reject(error);
  }
);

export default http;
