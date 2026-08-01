import { useEffect, useState, useCallback } from 'react'
import { Link } from 'react-router-dom'
import api from '../api/client.js'
import OpportunityCard from '../components/OpportunityCard.jsx'
import SearchFilter from '../components/SearchFilter.jsx'
import { InlineLoader } from '../components/Splash.jsx'
import { useAuth } from '../context/AuthContext.jsx'
import { CATEGORY_LABELS, CATEGORY_STYLES } from '../lib/constants.js'

export default function BoardPage() {
  const { user } = useAuth()
  const [items, setItems] = useState([])
  const [featured, setFeatured] = useState([])
  const [loading, setLoading] = useState(true)
  const [page, setPage] = useState(0)
  const [total, setTotal] = useState(0)
  const [filters, setFilters] = useState({ q: '', categories: [], workType: '', location: '', sort: 'newest' })

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
      {/* Hero + Search (TopCV style) */}
      <section
        className="hero-network overflow-hidden rounded-3xl bg-cover bg-center p-8 text-white shadow-card"
      >
        <h1 className="text-2xl font-extrabold md:text-3xl">Khám phá cơ hội dành cho sinh viên</h1>
        <p className="mt-2 max-w-xl text-sm text-white/85">
          Thực tập · Hackathon · Học bổng · Khởi nghiệp — tất cả trong một bảng tin.
        </p>
        <div className="mt-5">
          <SearchFilter onSearch={setFilters} />
        </div>
      </section>

      <div className="flex gap-6">
        {/* Side Bar (mẫu 1) */}
        <aside className="hidden w-56 shrink-0 lg:block">
          <div className="sticky top-20 space-y-4">
            <div className="rounded-2xl border border-slate-100 bg-white p-4 shadow-card">
              <h3 className="mb-2 text-sm font-bold text-slate-700">Danh mục</h3>
              <ul className="space-y-1">
                {Object.entries(CATEGORY_LABELS).map(([k, v]) => (
                  <li key={k}>
                    <button onClick={() => toggleCat(k)}
                      className={`flex w-full items-center justify-between rounded-lg px-2 py-1.5 text-left text-sm transition ${
                        filters.categories.includes(k) ? 'bg-brand-50 font-medium text-brand-700' : 'text-slate-600 hover:bg-slate-50'}`}>
                      <span style={{ color: 'inherit' }}>{v}</span>
                      {filters.categories.includes(k) && <span className="h-2 w-2 rounded-full bg-brand-500" />}
                    </button>
                  </li>
                ))}
              </ul>
            </div>
            <div className="rounded-2xl border border-slate-100 bg-white p-4 text-xs text-slate-400 shadow-card">
              Chọn danh mục để lọc nhanh bảng tin.
            </div>

            {user && (
              <div className="rounded-2xl border border-slate-100 bg-white p-4 shadow-card">
                <h3 className="mb-2 text-sm font-bold text-slate-700">Cá nhân</h3>
                <ul className="space-y-1">
                  <li><Link to="/me/bookmarks" className="block rounded-lg px-2 py-1.5 text-sm text-slate-600 hover:bg-slate-50">🔖 Đã lưu</Link></li>
                  <li><Link to="/me/applications" className="block rounded-lg px-2 py-1.5 text-sm text-slate-600 hover:bg-slate-50">📨 Đơn của tôi</Link></li>
                  <li><Link to="/me/notifications" className="block rounded-lg px-2 py-1.5 text-sm text-slate-600 hover:bg-slate-50">🔔 Thông báo</Link></li>
                </ul>
              </div>
            )}
          </div>
        </aside>

        {/* Body Area */}
        <div className="min-w-0 flex-1 space-y-6">
          {/* Featured */}
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
          <span className="text-xs text-slate-400">{items.length} / {total} kết quả</span>
        </div>

        {loading && items.length === 0 ? (
          <div className="py-16 text-center"><InlineLoader label="Đang tải bảng tin…" /></div>
        ) : items.length === 0 ? (
          <div className="py-16 text-center text-slate-400">Chưa có cơ hội nào phù hợp.</div>
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
