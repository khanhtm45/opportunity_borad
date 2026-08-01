/** Public asset under Vite `base` (GitHub Pages: /opportunity_borad/). */
export function asset(path) {
  const base = import.meta.env.BASE_URL || '/'
  return `${base}${String(path).replace(/^\//, '')}`
}
