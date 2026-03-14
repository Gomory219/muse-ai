import { fileURLToPath, URL } from 'node:url'

import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'
import vueDevTools from 'vite-plugin-vue-devtools'

// https://vite.dev/config/
export default defineConfig({
  plugins: [
    vue(),
    vueDevTools(),
  ],
  resolve: {
    alias: {
      '@': fileURLToPath(new URL('./src', import.meta.url))
    },
  },
  server: {
    proxy: {
      // 代理 /api 请求到后端服务器
      '/api': {
        target: 'http://localhost:7777',
        changeOrigin: true,
        // 不重写路径，因为后端本身就是 /api 开头
        // rewrite: (path) => path.replace(/^\/api/, '/api')
      }
    }
  }
})
