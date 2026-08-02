import { useEffect, useState, useCallback } from 'react'
import { Link } from 'react-router-dom'
import api from '../api/client.js'
import OpportunityCard from '../components/OpportunityCard.jsx'
import SearchFilter from '../components/SearchFilter.jsx'
import { InlineLoader } from '../components/Splash.jsx'
import { useAuth } from '../context/AuthContext.jsx'
import { CATEGORY_LABELS, CATEGORY_STYLES } from '../lib/constants.js'
import { asset } from '../lib/assets.js'

// Icon danh mục (svg path từ Heroicons outline)
const CAT_ICONS = {
  INTERNSHIP: 'M20.25 14.15v4.25a2.25 2.25 0 01-2.25 2.25h-15a2.25 2.25 0 01-2.25-2.25v-4.25m16.5 0a2.25 2.25 0 012.25 2.25M20.25 14.15l-9.75-6.75-9.75 6.75M3.75 14.15a2.25 2.25 0 00-2.25 2.25',
  STARTUP_RECRUITMENT: 'M15.59 14.37a6 6 0 01-5.84 7.38v-4.8m5.84-2.58a14.98 14.98 0 006.16-12.12A14.98 14.98 0 009.631 8.41m5.96 5.96a14.926 14.926 0 01-5.841 2.58m-.119-8.54a6 6 0 00-7.381 5.84h4.8m2.581-5.84a14.927 14.927 0 00-2.58 5.84m2.699 2.7c-.103.021-.207.041-.311.06a15.09 15.09 0 01-2.448-2.45 14.9 14.9 0 01.06-.312m-2.24 2.39a4.493 4.493 0 00-1.757 4.306 4.493 4.493 0 004.306-1.758M16.5 9a1.5 1.5 0 11-3 0 1.5 1.5 0 013 0z',
  INNOVATION_CONTEST: 'M12 18v-5.25m0 0a6.01 6.01 0 001.5-.189m-1.5.189a6.01 6.01 0 01-1.5-.189m3.75 7.478a12.06 12.06 0 01-4.5 0m3.75 2.383a14.406 14.406 0 01-3 0M14.25 18v-.192c0-.983.658-1.823 1.508-2.316a7.5 7.5 0 10-7.517 0c.85.493 1.509 1.333 1.509 2.316V18',
  HACKATHON: 'M17.25 6.75L22.5 12l-5.25 5.25m-10.5 0L1.5 12l5.25-5.25m7.5-3l-4.5 16.5',
  SCHOLARSHIP: 'M4.26 10.147a60.438 60.438 0 00-.491 6.347A48.627 48.627 0 0112 20.904a48.627 48.627 0 018.232-4.41 60.46 60.46 0 00-.491-6.347m-15.482 0a50.57 50.57 0 00-2.658-.813A59.905 59.905 0 0112 3.493a59.902 59.902 0 0110.399 5.84c-.896.248-1.783.52-2.658.814m-15.482 0A50.702 50.702 0 0112 13.489a50.702 50.702 0 017.74-3.342M6.75 15a.75.75 0 100-1.5.75.75 0 000 1.5zm0 0v-3.675A55.378 55.378 0 0112 8.443m-7.25 6.557A55.42 55.42 0 0112 15.557m-1.25-1.057a.75.75 0 01-1.5 0 .75.75 0 011.5 0zm7.75-3.557v3.675A55.378 55.378 0 0112 15.557m1.25-1.057a.75.75 0 10-1.5 0 .75.75 0 001.5 0z',
  INVESTMENT_FUND: 'M3 13.125C3 12.504 3.504 12 4.125 12h2.25c.621 0 1.125.504 1.125 1.125v6.75C7.5 20.496 6.996 21 6.375 21h-2.25A1.125 1.125 0 013 19.875v-6.75zM9.75 8.625c0-.621.504-1.125 1.125-1.125h2.25c.621 0 1.125.504 1.125 1.125v11.25c0 .621-.504 1.125-1.125 1.125h-2.25a1.125 1.125 0 01-1.125-1.125V8.625zM16.5 4.125c0-.621.504-1.125 1.125-1.125h2.25C20.496 3 21 3.504 21 4.125v15.75c0 .621-.504 1.125-1.125 1.125h-2.25a1.125 1.125 0 01-1.125-1.125V4.125z',
  INCUBATOR: 'M11.412 2.836a.75.75 0 011.176 0l3.746 4.357a.75.75 0 01-.585 1.22l-1.291-.618a.75.75 0 00-.352 1.264l2.055 1.545a.75.75 0 01-.45 1.35l-1.48.678a.75.75 0 00.45 1.35l.02.009a.75.75 0 01-.692 1.328l-4.999-2.087a3 3 0 01-2.105-3.618l2.196-4.78a.75.75 0 011.41.356l-.79 2.754a.75.75 0 001.155.46l1.682-.806z',
}
const ICON_BOOKMARK = 'M17.593 3.322c1.1.128 1.907 1.077 1.907 2.185V21L12 17.25 4.5 21V5.507c0-1.108.806-2.057 1.907-2.185a48.507 48.507 0 0111.186 0z'
const ICON_DOC = 'M19.5 14.25v-2.625a3.375 3.375 0 00-3.375-3.375h-1.5A1.125 1.125 0 0113.5 7.125v-1.5a3.375 3.375 0 00-3.375-3.375H8.25m2.25 0H5.625c-.621 0-1.125.504-1.125 1.125v17.25c0 .621.504 1.125 1.125 1.125h12.75c.621 0 1.125-.504 1.125-1.125V11.25a9 9 0 00-9-9z'
const ICON_BELL = 'M14.857 17.082a23.848 23.848 0 005.454-1.31A8.967 8.967 0 0118 9.75V9A6 6 0 006 9v.75a8.967 8.967 0 01-2.312 6.022c1.733.64 3.56 1.085 5.455 1.31m5.714 0a24.255 24.255 0 01-5.714 0m5.714 0a3 3 0 11-5.714 0'
const ICON_BOLT = 'M3.75 13.5l10.5-11.25L12 10.5h8.25L9.75 21.75 12 13.5H3.75z'

export default function BoardPage() {
  const { user } = useAuth()
  const [items, setItems] = useState([])
  const [featured, setFeatured] = useState([])
  const [loading, setLoading] = useState(true)
  const [page, setPage] = useState(0)
  const [total, setTotal] = useState(0)
  const [filters, setFilters] = useState({ q: '', categories: [], orgName: '', workType: '', location: '', sort: 'newest' })

  const loadFeatured = async () => {
    try {
      const { data } = await api.get('/opportunities/featured')
      setFeatured(data || [])
    } catch {}
  }

  const load = useCallback(async (nextPage = 0, replace = true) => {
    setLoading(true)
    try {
      const params = new URLSearchParams()
      params.set('page', String(nextPage))
      params.set('size', '20')
      if (filters.q) params.set('q', filters.q)
      if (filters.orgName) params.set('orgName', filters.orgName)
      filters.categories.forEach((c) => params.append('categories', c))
      if (filters.workType) params.set('workType', filters.workType)
      if (filters.location) params.set('location', filters.location)
      if (filters.sort) params.set('sort', filters.sort)
      const { data } = await api.get(`/opportunities?${params.toString()}`)
      const list = data.items || []
      setItems((prev) => (replace ? list : [...prev, ...list]))
      setTotal(data.total || 0)
      setPage(nextPage)
    } catch (e) {
      console.error(e)
    } finally {
      setLoading(false)
    }
  }, [filters])

  useEffect(() => { loadFeatured() }, [])
  useEffect(() => { load(0, true) }, [filters])

  const hasMore = items.length < total

  const toggleCat = (c) => {
    setFilters((f) => ({
      ...f,
      categories: f.categories.includes(c) ? f.categories.filter((x) => x !== c) : [...f.categories, c],
    }))
  }

  return (
    <div className="space-y-6">
      {/* Hero + Search (dark, infinity glow) */}
      <section
        className="relative overflow-hidden rounded-3xl bg-slate-900 bg-cover bg-center p-8 text-white shadow-card"
        style={{ backgroundImage: `url(${asset('ob-network.svg')})` }}
      >
        {/* Infinity glow bên phải */}
        <div className="pointer-events-none absolute -right-10 top-1/2 hidden -translate-y-1/2 md:block">
          <svg width="320" height="320" viewBox="0 0 200 200" fill="none" className="opacity-80">
            <defs>
              <linearGradient id="infG" x1="0" y1="0" x2="1" y2="1">
                <stop offset="0%" stopColor="#38bdf8" />
                <stop offset="100%" stopColor="#f69022" />
              </linearGradient>
              <filter id="infBlur"><feGaussianBlur stdDeviation="3" /></filter>
            </defs>
            <path filter="url(#infBlur)" stroke="url(#infG)" strokeWidth="10" strokeLinecap="round" fill="none"
              d="M100 60 a40 40 0 1 1 -28 68 a30 30 0 1 0 28 -68 a30 30 0 1 0 -28 68 a40 40 0 1 1 28 -68 Z" />
          </svg>
        </div>

        <div className="relative">
          <h1 className="text-2xl font-extrabold drop-shadow md:text-3xl">Khám phá cơ hội dành cho sinh viên</h1>
          <p className="mt-2 max-w-xl text-sm text-white/90">
            Thực tập · Hackathon · Học bổng · Khởi nghiệp — tất cả trong một bảng tin.
          </p>
          <div className="mt-5">
            <SearchFilter onSearch={setFilters} />
          </div>
        </div>
      </section>

      <div className="flex gap-6">
        {/* Side Bar */}
        <aside className="hidden w-56 shrink-0 lg:block">
          <div className="sticky top-20 space-y-4">
            <div className="rounded-2xl border border-slate-100 bg-white p-4 shadow-card">
              <h3 className="mb-2 text-sm font-bold text-slate-700">Danh mục</h3>
              <ul className="space-y-1">
                {Object.entries(CATEGORY_LABELS).map(([k, v]) => (
                  <li key={k}>
                    <button onClick={() => toggleCat(k)}
                      className={`flex w-full items-center justify-between gap-2 rounded-lg px-2 py-1.5 text-left text-sm transition ${
                        filters.categories.includes(k) ? 'bg-brand-50 font-medium text-brand-700' : 'text-slate-700 hover:bg-slate-50'}`}>
                      <span className="flex items-center gap-2">
                        <svg className="h-4 w-4 text-brand-500" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.6">
                          <path strokeLinecap="round" strokeLinejoin="round" d={CAT_ICONS[k] || CAT_ICONS.INTERNSHIP} />
                        </svg>
                        {v}
                      </span>
                      {filters.categories.includes(k) && <span className="h-2 w-2 rounded-full bg-brand-500" />}
                    </button>
                  </li>
                ))}
              </ul>
            </div>

            <div className="flex items-start gap-2 rounded-2xl border border-slate-100 bg-white p-4 text-xs text-slate-500 shadow-card">
              <svg className="mt-0.5 h-4 w-4 shrink-0 text-gold-500" viewBox="0 0 24 24" fill="currentColor">
                <path d={ICON_BOLT} />
              </svg>
              <span>Chọn danh mục để lọc nhanh bảng tin.</span>
            </div>

            {user && (
              <div className="rounded-2xl border border-slate-100 bg-white p-4 shadow-card">
                <h3 className="mb-2 text-sm font-bold text-slate-700">Cá nhân</h3>
                <ul className="space-y-1">
                  <li>
                    <Link to="/me/bookmarks" className="flex items-center gap-2 rounded-lg px-2 py-1.5 text-sm text-slate-700 hover:bg-slate-50">
                      <svg className="h-4 w-4 text-brand-500" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.6"><path strokeLinecap="round" strokeLinejoin="round" d={ICON_BOOKMARK} /></svg>
                      Đã lưu
                    </Link>
                  </li>
                  <li>
                    <Link to="/me/applications" className="flex items-center gap-2 rounded-lg px-2 py-1.5 text-sm text-slate-700 hover:bg-slate-50">
                      <svg className="h-4 w-4 text-brand-500" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.6"><path strokeLinecap="round" strokeLinejoin="round" d={ICON_DOC} /></svg>
                      Đơn của tôi
                    </Link>
                  </li>
                  <li>
                    <Link to="/me/notifications" className="flex items-center gap-2 rounded-lg px-2 py-1.5 text-sm text-slate-700 hover:bg-slate-50">
                      <svg className="h-4 w-4 text-brand-500" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.6"><path strokeLinecap="round" strokeLinejoin="round" d={ICON_BELL} /></svg>
                      Thông báo
                    </Link>
                  </li>
                </ul>
              </div>
            )}
          </div>
        </aside>

        {/* Body Area */}
        <div className="min-w-0 flex-1 space-y-6">
          {featured.length > 0 && (
            <section>
              <h2 className="mb-3 text-lg font-bold text-slate-800">🔥 Cơ hội nổi bật</h2>
              <div className="grid grid-cols-1 gap-4 sm:grid-cols-2 lg:grid-cols-3">
                {featured.map((o) => <OpportunityCard key={o.oppId} opp={o} />)}
              </div>
            </section>
          )}

          <section>
            <div className="mb-3 flex items-center justify-between">
              <h2 className="text-lg font-bold text-slate-800">Tất cả cơ hội</h2>
              <span className="text-xs text-slate-500">{items.length} / {total} kết quả</span>
            </div>

            {loading && items.length === 0 ? (
              <div className="py-16 text-center"><InlineLoader label="Đang tải bảng tin…" /></div>
            ) : items.length === 0 ? (
              <div className="py-16 text-center text-slate-500">Chưa có cơ hội nào phù hợp.</div>
            ) : (
              <div className="grid grid-cols-1 gap-4 sm:grid-cols-2 lg:grid-cols-3">
                {items.map((o) => <OpportunityCard key={o.oppId} opp={o} />)}
              </div>
            )}

            {hasMore && (
              <div className="mt-6 text-center">
                <button onClick={() => load(page + 1, false)} className="btn-ghost" disabled={loading}>
                  {loading ? 'Đang tải…' : 'Xem thêm'}
                </button>
              </div>
            )}
          </section>
        </div>
      </div>
    </div>
  )
}
