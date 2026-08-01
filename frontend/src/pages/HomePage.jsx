import { useState, useEffect } from 'react'
import { Link } from 'react-router-dom'
import api from '../api/client.js'
import { useAuth } from '../context/AuthContext.jsx'
import { CATEGORY_LABELS, CATEGORY_STYLES } from '../lib/constants.js'
import NetworkBg from '../components/NetworkBg.jsx'
import { InlineLoader } from '../components/Splash.jsx'

const FEATURE_BOXES = [
  { t: 'Hackathon', d: 'Cuộc thi đổi mới sáng tạo, giải thưởng lớn.', c: 'bg-brand-50 text-brand-700', to: '/explore?cat=HACKATHON' },
  { t: 'Học bổng', d: 'Hỗ trợ tài chính cho sinh viên xuất sắc.', c: 'bg-gold-50 text-gold-700', to: '/explore?cat=SCHOLARSHIP' },
  { t: 'Thực tập', d: 'Cơ hội thực tập tại doanh nghiệp, startup.', c: 'bg-acent-50 text-acent-700', to: '/explore?cat=INTERNSHIP' },
]

export default function HomePage() {
  const { user } = useAuth()
  const [featured, setFeatured] = useState([])
  const [loading, setLoading] = useState(true)

  useEffect(() => {
    api.get('/opportunities/featured').then((r) => setFeatured(r.data?.items || [])).catch(() => {}).finally(() => setLoading(false))
  }, [])

  return (
    <div className="space-y-8">
      {/* Banner (mẫu 2: Header / Banner) */}
      <NetworkBg className="overflow-hidden rounded-3xl p-10 text-white shadow-card" overlay="bg-slate-950/10">
        <h1 className="text-3xl font-extrabold drop-shadow md:text-4xl">Opportunity Board</h1>
        <p className="mt-3 max-w-2xl text-white/90">
          Bảng tin cơ hội sinh viên — nơi hội tụ thực tập, hackathon, học bổng và khởi nghiệp.
          Khám phá, lưu và ứng tuyển chỉ với vài cú click.
        </p>
        <div className="mt-5 flex flex-wrap gap-3">
          <Link to="/board" className="btn-primary">Khám phá cơ hội →</Link>
          {!user && <Link to="/register" className="btn-ghost bg-white/90">Tạo tài khoản</Link>}
        </div>
      </NetworkBg>

      {/* Intro Text Area */}
      <section className="mx-auto max-w-3xl text-center">
        <h2 className="text-xl font-bold text-slate-800">Dành cho sinh viên & nhà tuyển dụng</h2>
        <p className="mt-2 text-sm text-slate-500">
          Sinh viên dễ dàng tìm cơ hội phù hợp. Nhà tuyển dụng đăng tin được kiểm duyệt,
          theo dõi ứng viên và xây dựng thương hiệu tuyển dụng.
        </p>
      </section>

      {/* 3 Box */}
      <section className="grid grid-cols-1 gap-4 md:grid-cols-3">
        {FEATURE_BOXES.map((b) => (
          <Link key={b.t} to={b.to} className={`rounded-2xl border border-slate-100 p-6 shadow-card transition hover:-translate-y-0.5 ${b.c}`}>
            <h3 className="text-lg font-bold">{b.t}</h3>
            <p className="mt-1 text-sm opacity-80">{b.d}</p>
          </Link>
        ))}
      </section>

      {/* Cơ hội nổi bật (TopCV: gợi ý) */}
      <section>
        <div className="mb-3 flex items-center justify-between">
          <h2 className="text-lg font-bold text-slate-800">🔥 Cơ hội nổi bật</h2>
          <Link to="/board" className="text-sm text-brand-600 hover:underline">Xem tất cả →</Link>
        </div>
        {loading ? <InlineLoader /> : featured.length === 0 ? (
          <p className="py-8 text-center text-sm text-slate-400">Chưa có cơ hội nổi bật.</p>
        ) : (
          <div className="grid grid-cols-1 gap-4 sm:grid-cols-2 lg:grid-cols-3">
            {featured.slice(0, 6).map((o) => (
              <Link key={o.oppId} to={`/opportunities/${o.slug}`}
                className="rounded-2xl border border-slate-100 bg-white p-5 shadow-card transition hover:-translate-y-0.5">
                <div className="flex items-center gap-3">
                  {o.logoUrl ? <img src={o.logoUrl} alt="" className="h-10 w-10 rounded-xl object-cover" />
                    : <div className="flex h-10 w-10 items-center justify-center rounded-xl bg-brand-gradient text-sm font-bold text-white">{(o.orgName||'?').charAt(0)}</div>}
                  <div className="min-w-0">
                    <p className="truncate text-sm font-bold text-slate-800">{o.title}</p>
                    <p className="truncate text-xs text-slate-400">{o.orgName}</p>
                  </div>
                </div>
                <div className="mt-3 flex flex-wrap gap-2">
                  <span className={`chip ${CATEGORY_STYLES[o.categoryCode] || 'bg-slate-100 text-slate-600'}`}>{CATEGORY_LABELS[o.categoryCode]}</span>
                  {o.isFeatured && <span className="chip bg-gold-50 text-gold-700">🔥 Nổi bật</span>}
                </div>
              </Link>
            ))}
          </div>
        )}
      </section>

      {/* Category quick links */}
      <section>
        <h2 className="mb-3 text-lg font-bold text-slate-800">Danh mục phổ biến</h2>
        <div className="flex flex-wrap gap-2">
          {Object.entries(CATEGORY_LABELS).map(([k, v]) => (
            <Link key={k} to={`/explore?cat=${k}`} className="chip border border-slate-200 bg-white text-slate-600 hover:border-brand-300 hover:text-brand-700">
              {v}
            </Link>
          ))}
        </div>
      </section>
    </div>
  )
}
