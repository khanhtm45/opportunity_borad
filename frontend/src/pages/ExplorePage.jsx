import { useState, useEffect, useCallback } from 'react'
import { Link, useSearchParams } from 'react-router-dom'
import api from '../api/client.js'
import { CATEGORY_LABELS, CATEGORY_STYLES } from '../lib/constants.js'
import { InlineLoader } from '../components/Splash.jsx'

export default function ExplorePage() {
  const [params] = useSearchParams()
  const cat = params.get('cat')
  const [items, setItems] = useState([])
  const [loading, setLoading] = useState(true)

  const load = useCallback(async () => {
    setLoading(true)
    try {
      const p = new URLSearchParams()
      p.set('size', '12')
      if (cat) p.set('categories', cat)
      const { data } = await api.get(`/opportunities?${p.toString()}`)
      setItems(data.items || [])
    } catch {} finally { setLoading(false) }
  }, [cat])

  useEffect(() => { load() }, [load])

  // 12 ô danh mục (grid)
  const cats = Object.entries(CATEGORY_LABELS)

  return (
    <div className="space-y-6">
      {/* Header / Banner */}
      <div className="rounded-3xl bg-brand-gradient p-8 text-white shadow-card">
        <h1 className="text-2xl font-extrabold md:text-3xl">Khám phá danh mục</h1>
        <p className="mt-2 text-sm text-white/85">Tìm cơ hội theo lĩnh vực bạn quan tâm.</p>
      </div>

      {/* Grid 12 ô (mẫu 4) */}
      <section>
        <h2 className="mb-3 text-lg font-bold text-slate-800">Danh mục ({cats.length})</h2>
        <div className="grid grid-cols-2 gap-3 sm:grid-cols-3 lg:grid-cols-4">
          {cats.map(([k, v]) => (
            <Link key={k} to={`/explore?cat=${k}`}
              className={`rounded-2xl border border-slate-100 p-4 text-center shadow-card transition hover:-translate-y-0.5 ${CATEGORY_STYLES[k] || 'bg-slate-50 text-slate-700'}`}>
              <span className="text-sm font-semibold">{v}</span>
            </Link>
          ))}
        </div>
      </section>

      {/* Kết quả / 3 Box featured */}
      <section>
        <h2 className="mb-3 text-lg font-bold text-slate-800">
          {cat ? `Kết quả: ${CATEGORY_LABELS[cat] || cat}` : 'Cơ hội nổi bật'}
        </h2>
        {loading ? <InlineLoader /> : items.length === 0 ? (
          <div className="py-12 text-center text-slate-400">Chưa có cơ hội nào.</div>
        ) : (
          <div className="grid grid-cols-1 gap-4 sm:grid-cols-2 lg:grid-cols-3">
            {items.slice(0, 9).map((o) => (
              <Link key={o.oppId} to={`/opportunities/${o.slug}`}
                className="rounded-2xl border border-slate-100 bg-white p-5 shadow-card transition hover:-translate-y-0.5">
                <span className={`chip ${CATEGORY_STYLES[o.categoryCode] || 'bg-slate-100'}`}>{CATEGORY_LABELS[o.categoryCode]}</span>
                <h3 className="mt-2 font-bold text-slate-800">{o.title}</h3>
                <p className="text-xs text-slate-400">{o.orgName}</p>
              </Link>
            ))}
          </div>
        )}
      </section>
    </div>
  )
}
