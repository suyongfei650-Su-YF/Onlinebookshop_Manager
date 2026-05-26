import { defineConfig, loadEnv } from 'vite'
import vue from '@vitejs/plugin-vue'

// https://vite.dev/config/
export default defineConfig(({ mode }) => {
  const env = loadEnv(mode, process.cwd(), '')
  const backend = env.VITE_PROXY_TARGET || 'http://127.0.0.1:8080'
  const ctx = (env.VITE_BACKEND_CONTEXT || '/Onlinebookshop_Manager').replace(/\/$/, '')

  return {
    plugins: [vue()],
    appType: 'spa',
    server: {
      proxy: {
        // 推荐：统一走 /api，由 Vite 转发到 Tomcat/SpringBoot
        '/api': {
          target: backend,
          changeOrigin: true,
          rewrite: (p) => `${ctx}${p}`,
          cookiePathRewrite: { '*': '/' },
        },
        '/Onlinebookshop_Manager/api': {
          target: backend,
          changeOrigin: true,
        },
        '/Onlinebookshop_Manager_war/api': {
          target: backend,
          changeOrigin: true,
        },
        '/Onlinebookshop_Manager/book-covers': {
          target: backend,
          changeOrigin: true,
        },
        '/Onlinebookshop_Manager_war/book-covers': {
          target: backend,
          changeOrigin: true,
        },
        '/book-covers': {
          target: backend,
          changeOrigin: true,
          rewrite: (p) => `${ctx}${p}`,
        },
      },
    },
  }
})
