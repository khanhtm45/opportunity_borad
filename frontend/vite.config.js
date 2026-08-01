import { defineConfig } from 'vite'
import react from '@vitejs/plugin-react'

// Dev proxy: /api -> backend Spring Boot trên 8080
// GitHub Pages: base=/opportunity_borad/
// DigitalOcean static site: set VITE_BASE_PATH=/
export default defineConfig({
  base: process.env.VITE_BASE_PATH || '/opportunity_borad/',
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
