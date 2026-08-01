import { defineConfig } from 'vite'
import react from '@vitejs/plugin-react'

// Dev proxy: /api -> backend Spring Boot trên 8080
// base khớp GitHub Pages: https://khanhtm45.github.io/opportunity_borad/
export default defineConfig({
  base: '/opportunity_borad/',
  plugins: [react()],
  server: {
    port: 5173,
    proxy: {
      '/api': {
        target: 'http://localhost:8080',
        changeOrigin: true,
      },
    },
  },
})
