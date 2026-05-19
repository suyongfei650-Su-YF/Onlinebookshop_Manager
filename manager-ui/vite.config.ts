import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'

// https://vite.dev/config/
export default defineConfig({
  plugins: [vue()],
  server: {
    proxy: {
      // 与 WAR 部署上下文一致，便于本地联调 Java 后端
      '/Onlinebookshop_Manager/api': {
        target: 'http://127.0.0.1:8080',
        changeOrigin: true,
      },
      // 兼容 IDEA 默认 WAR artifact 上下文（Onlinebookshop_Manager_war）
      '/Onlinebookshop_Manager_war/api': {
        target: 'http://127.0.0.1:8080',
        changeOrigin: true,
      },
      '/Onlinebookshop_Manager/book-covers': {
        target: 'http://127.0.0.1:8080',
        changeOrigin: true,
      },
      '/Onlinebookshop_Manager_war/book-covers': {
        target: 'http://127.0.0.1:8080',
        changeOrigin: true,
      },
    },
  },
})
