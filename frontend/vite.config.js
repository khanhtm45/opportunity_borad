import { defineConfig } from 'vite'
import react from '@vitejs/plugin-react'

/**
 * Vite `base` = path prefix for JS/CSS assets (NOT the API URL).
 * - DigitalOcean FE app: /
 * - GitHub Pages: /opportunity_borad/
 * Reject absolute URLs / api/v1 mistaken for base (causes 403 + blank MIME).
 */
function resolveBase() {
  const raw = (process.env.VITE_BASE_PATH || '').trim()
  if (raw && !/^https?:\/\//i.test(raw) && !/api\/v1/i.test(raw)) {
    return raw.endsWith('/') ? raw : `${raw}/`
  }
  return '/opportunity_borad/'
}

// Dev proxy: /api -> backend Spring Boot trên 8080
export default defineConfig({
  base: resolveBase(),
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
