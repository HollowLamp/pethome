import { defineConfig } from "vite";
import react from "@vitejs/plugin-react";

// https://vite.dev/config/
export default defineConfig({
  plugins: [
    react({
      babel: {
        plugins: [["babel-plugin-react-compiler"]],
      },
    }),
  ],
  server: {
    proxy: {
      // 代理文件请求到后端
      "/files": {
        target: "http://localhost:8080",
        changeOrigin: true,
      },
    },
  },
});
