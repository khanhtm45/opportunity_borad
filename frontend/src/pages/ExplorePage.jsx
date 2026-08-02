import { useState, useEffect, useCallback } from 'react'
import { Link, useSearchParams } from 'react-router-dom'
import api from '../api/client.js'
import OpportunityCard from '../components/OpportunityCard.jsx'
import { InlineLoader } from '../components/Splash.jsx'
import { useAuth } from '../context/AuthContext.jsx'
import { CATEGORY_LABELS, CATEGORY_STYLES } from '../lib/constants.js'
import { asset } from '../lib/assets.js'

// Icon danh mục (heroicons outline)
const CAT_ICONS = {
  INTERNSHIP: 'M20.25 14.15v4.25a2.25 2.25 0 01-2.25 2.25h-15a2.25 2.25 0 01-2.25-2.25v-4.25m16.5 0a2.25 2.25 0 012.25 2.25M20.25 14.15l-9.75-6.75-9.75 6.75M3.75 14.15a2.25 2.25 0 00-2.25 2.25',
  STARTUP_RECRUITMENT: 'M15.59 14.37a6 6 0 01-5.84 7.38v-4.8m5.84-2.58a14.98 14.98 0 006.16-12.12A14.98 14.98 0 009.631 8.41m5.96 5.96a14.926 14.926 0 01-5.841 2.58m-.119-8.54a6 6 0 00-7.381 5.84h4.8m2.581-5.84a14.927 14.927 0 00-2.58 5.84m2.699 2.7c-.103.021-.207.041-.311.06a15.09 15.09 0 01-2.448-2.45 14.9 14.9 0 01.06-.312m-2.24 2.39a4.493 4.493 0 00-1.757 4.306 4.493 4.493 0 004.306-1.758M16.5 9a1.5 1.5 0 11-3 0 1.5 1.5 0 013 0z',
  INNOVATION_CONTEST: 'M12 18v-5.25m0 0a6.01 6.01 0 001.5-.189m-1.5.189a6.01 6.01 0 01-1.5-.189m3.75 7.478a12.06 12.06 0 01-4.5 0m3.75 2.383a14.406 14.406 0 01-3 0M14.25 18v-.192c0-.983.658-1.823 1.508-2.316a7.5 7.5 0 10-7.517 0c.85.493 1.509 1.333 1.509 2.316V18',
  HACKATHON: 'M17.25 6.75L22.5 12l-5.25 5.25m-10.5 0L1.5 12l5.25-5.25m7.5-3l-4.5 16.5',
  SCHOLARSHIP: 'M4.26 10.147a60.438 60.438 0 00-.491 6.347A48.627 48.627 0 0112 20.904a48.627 48.627 0 018.232-4.41 60.46 60.46 0 00-.491-6.347m-15.482 0a50.57 50.57 0 00-2.658-.813A59.905 59.905 0 0112 3.493a59.902 59.902 0 0110.399 5.84c-.896.248-1.783.52-2.658.814m-15.482 0A50.702 50.702 0 0112 13.489a50.702 50.702 0 017.74-3.342M6.75 15a.75.75 0 100-1.5.75.75 0 000 1.5zm0 0v-3.675A55.378 55.378 0 0112 8.443m-7.25 6.557A55.42 55.42 0 0112 15.557m-1.25-1.057a.75.75 0 01-1.5 0 .75.75 0 011.5 0zm7.75-3.557v3.675A55.378 55.378 0 0112 15.557m1.25-1.057a.75.75 0 10-1.5 0 .75.75 0 001.5 0z',
  INVESTMENT_FUND: 'M3 13.125C3 12.504 3.504 12 4.125 12h2.25c.621 0 1.125.504 1.125 1.125v6.75C7.5 20.496 6.996 21 6.375 21h-2.25A1.125 1.125 0 013 19.875v-6.75zM9.75 8.625c0-.621.504-1.125 1.125-1.125h2.25c.621 0 1.125.504 1.125 1.125v11.25c0 .621-.504 1.125-1.125 1.125h-2.25a1.125 1.125 0 01-1.125-1.125V8.625zM16.5 4.125c0-.621.504-1.125 1.125-1.125h2.25C20.496 3 21 3.504 21 4.125v15.75c0 .621-.504 1.125-1.125 1.125h-2.25a1.125 1.125 0 01-1.125-1.125V4.125z',
  INCUBATOR: 'M11.412 2.836a.75.75 0 011.176 0l3.746 4.357a.75.75 0 01-.585 1.22l-1.291-.618a.75.75 0 00-.352 1.264l2.055 1.545a.75.75 0 01-.45 1.35l-1.48.678a.75.75 0 00.45 1.35l.02.009a.75.75 0 01-.692 1.328l-4.999-2.087a3 3 0 01-2.105-3.618l2.196-4.78a.75.75 0 011.41.356l-.79 2.754a.75.75 0 001.155.46l1.682-.806z',
}

export default function ExplorePage() {
  const { user } = useAuth()
  const [params, setParams] = useSearchParams()
  const cat = params.get('cat')
  const [items, setItems] = useState([])
  const [featured, setFeatured] = useState([])
  const [loading, setLoading] = useState(true)

  const loadFeatured = async () => {
    try {
      const { data } = await api.get('/opportunities/featured')
      setFeatured(Array.isArray(data) ? data : (data?.items || []))
    } catch {}
  }

  const load = useCallback(async () => {
    setLoading(true)
    try {
      const p = new URLSearchParams()
      p.set('size', '20')
      if (cat) p.set('categories', cat)
      const { data } = await api.get(`/opportunities?${p.toString()}`)
      setItems(data.items || [])
    } catch {} finally { setLoading(false) }
  }, [cat])

  useEffect(() => { loadFeatured() }, [])
  useEffect(() => { load() }, [load])

  const pickCat = (c) => setParams(c ? { cat: c } : {})

  const cats = Object.entries(CATEGORY_LABELS)

  return (
    <div className="space-y-6">
      {/* Banner (dark, nhẹ) */}
      <section
        className="relative overflow-hidden rounded-3xl bg-slate-900 bg-cover bg-center p-8 text-white shadow-card"
        style={{ backgroundImage: `url(${asset('ob-network.svg')})` }}
      >
        <div className="pointer-events-none absolute -right-10 top-1/2 hidden -translate-y-1/2 md:block">
          <svg width="300" height="300" viewBox="0 0 200 200" fill="none" className="opacity-70">
            <defs>
              <linearGradient id="infG3" x1="0" y1="0" x2="1" y2="1">
                <stop offset="0%" stopColor="#38bdf8" />
                <stop offset="100%" stopColor="#f69022" />
              </linearGradient>
              <filter id="infBlur3"><feGaussianBlur stdDeviation="3" /></filter>
            </defs>
            <path filter="url(#infBlur3)" stroke="url(#infG3)" strokeWidth="10" strokeLinecap="round" fill="none"
              d="M100 60 a40 40 0 1 1 -28 68 a30 30 0 1 0 28 -68 a30 30 0 1 0 -28 68 a40 40 0 1 1 28 -68 Z" />
          </svg>
        </div>
        <div className="relative max-w-2xl">
          <h1 className="text-2xl font-extrabold drop-shadow md:text-3xl">Khám phá theo lĩnh vực</h1>
          <p className="mt-2 text-sm text-white/85">
            Chọn một lĩnh vực để xem hàng trăm cơ hội thực tập, học bổng, khởi nghiệp phù hợp với bạn.
          </p>
        </div>
      </section>

      {/* GRID DANH MỤC LỚN (điểm nhấn Khám phá) */}
      <section>
        <h2 className="mb-3 text-lg font-bold text-slate-800">Danh mục cơ hội</h2>
        <div className="grid grid-cols-2 gap-3 sm:grid-cols-3 lg:grid-cols-4">
          {cats.map(([k, v]) => {
            const active = cat === k
            return (
              <button key={k} onClick={() => pickCat(active ? '' : k)}
                className={`group flex flex-col items-center gap-3 rounded-2xl border p-6 text-center shadow-card transition hover:-translate-y-0.5 ${
                  active ? 'border-brand-400 bg-brand-50' : 'border-slate-100 bg-white hover:border-brand-200'}`}>
                <span className={`flex h-12 w-12 items-center justify-center rounded-2xl ${CATEGORY_STYLES[k] || 'bg-slate-100 text-slate-600'}`}>
                  <svg className="h-6 w-6" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.6">
                    <path strokeLinecap="round" strokeLinejoin="round" d={CAT_ICONS[k] || CAT_ICONS.INTERNSHIP} />
                  </svg>
                </span>
                <span className="text-sm font-semibold text-slate-700">{v}</span>
              </button>
            )
          })}
        </div>
      </section>

      {/* Kết quả theo danh mục đã chọn */}
      <section>
        <div className="mb-3 flex items-center justify-between">
          <h2 className="text-lg font-bold text-slate-800">
            {cat ? `Kết quả: ${CATEGORY_LABELS[cat] || cat}` : 'Cơ hội nổi bật'}
          </h2>
          {cat && (
            <button onClick={() => pickCat('')} className="text-xs text-brand-600 hover:underline">Xóa bộ lọc</button>
          )}
        </div>

        {loading ? (
          <div className="py-16 text-center"><InlineLoader label="Đang tải…" /></div>
        ) : items.length === 0 ? (
          <div className="py-16 text-center text-slate-400">Chưa có cơ hội nào phù hợp.</div>
        ) : (
          <div className="grid grid-cols-1 gap-4 sm:grid-cols-2 lg:grid-cols-3">
            {items.map((o) => <OpportunityCard key={o.oppId} opp={o} />)}
          </div>
        )}
      </section>
    </div>
  )
}
