import { create } from "zustand";
import { persist } from "zustand/middleware";

// 创建认证状态管理 store
const useAuthStore = create(
  persist(
    (set) => ({
      // 登录状态
      isLoggedIn: false,
      // 用户信息
      user: null,
      // 登录方法
      login: (userData) => {
        set({
          isLoggedIn: true,
          user: userData,
        });
      },
      // 登出方法
      logout: () => {
        set({
          isLoggedIn: false,
          user: null,
        });
      },
    }),
    {
      name: "auth-storage", // localStorage 的 key
    }
  )
);

export default useAuthStore;
