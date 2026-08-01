import { useEffect, useState } from 'react'
import { useParams, useNavigate, Link } from 'react-router-dom'
import api from '../api/client.js'
import { useAuth } from '../context/AuthContext.jsx'
import {
  CATEGORY_LABELS, CATEGORY_STYLES, STATUS_LABELS, STATUS_STYLES,
  WORKTYPE_LABELS, LOCATION_LABELS, fmtDate, fmtDateTime,
} from '../lib/constants.js'
import { InlineLoader } from '../components/Splash.jsx'
import { asset } from '../lib/assets.js'

export default function DetailPage() {
  const { slug } = useParams()
  const { user } = useAuth()
  const navigate = useNavigate()
  const [opp, setOpp] = useState(null)
  const [related, setRelated] = useState([])
  const [loading, setLoading] = useState(true)
  const [saved, setSaved] = useState(false)
  const [msg, setMsg] = useState('')

  useEffect(() => {
    setLoading(true)
    api.get(`/opportunities/${slug}`)
      .then((r) => {
        setOpp(r.data)
        setRelated(r.data.related || [])
        setSaved(!!r.data.savedByMe)
      })
      .catch(() => setOpp(null))
      .finally(() => setLoading(false))
  }, [slug])

  const toggleSave = async () => {
    if (!user) return navigate('/login')
    try {
      if (saved) {
        await api.delete(`/opportunities/${opp.oppId}/save`)
        setSaved(false)
      } else {
        await api.post(`/opportunities/${opp.oppId}/save`, { notifyBeforeHours: 48 })
        setSaved(true)
      }
    } catch (e) { setMsg(e.response?.data?.error?.message || 'Lỗi') }
  }

  const apply = async () => {
    if (!user) return navigate('/login')
    if (user.role !== 'STUDENT') return setMsg('Chỉ sinh viên mới được ứng tuyển')
    try {
      await api.post(`/opportunities/${opp.oppId}/apply`)
      setMsg('✅ Đã nộp hồ sơ thành công!')
    } catch (e) {
      setMsg(e.response?.data?.error?.message || 'Lỗi khi nộp')
    }
  }

  const external = () => {
    if (!user) return navigate('/login')
    window.open(opp.externalLink, '_blank')
    api.post(`/opportunities/${opp.oppId}/external-click`).catch(() => {})
  }

  const share = async () => {
    try {
      await api.post(`/opportunities/${opp.oppId}/share`)
      const url = window.location.href
      if (navigator.clipboard?.writeText) await navigator.clipboard.writeText(url)
      setMsg('🔗 Đã copy link chia sẻ')
      setOpp((prev) => prev ? { ...prev, shareCount: (prev.shareCount || 0) + 1 } : prev)
    } catch (e) {
      setMsg(e.response?.data?.error?.message || 'Không chia sẻ được')
    }
  }

  if (loading) return <div className="py-16 text-center"><InlineLoader label="Đang tải…" /></div>
  if (!opp) return <div className="py-16 text-center text-slate-400">Không tìm thấy cơ hội.</div>

  const isExternal = opp.applyMode === 'EXTERNAL'

  return (
    <div className="grid gap-6 lg:grid-cols-3">
      <div className="lg:col-span-2 space-y-5">
        <button onClick={() => navigate(-1)} className="text-sm text-slate-500 hover:text-brand-600">← Quay lại</button>
        <div
          className="rounded-2xl border border-slate-100 bg-slate-900 bg-cover bg-center p-6 text-white shadow-card"
          style={{ backgroundImage: `url(${opp.bannerUrl || asset('ob-network.svg')})` }}
        >
          <div className="flex items-start gap-4">
            {opp.logoUrl ? <img src={opp.logoUrl} alt="" className="h-14 w-14 rounded-2xl object-cover" />
              : <div className="flex h-14 w-14 items-center justify-center rounded-2xl bg-brand-gradient font-bold text-white">{(opp.orgName||'?').charAt(0)}</div>}
            <div>
              <h1 className="text-xl font-extrabold text-white drop-shadow">{opp.title}</h1>
              <p className="text-sm text-white/80">{opp.orgName}</p>
            </div>
          </div>

          <div className="mt-4 flex flex-wrap gap-2 text-xs">
            <span className={`chip ${CATEGORY_STYLES[opp.categoryCode] || 'bg-slate-100'}`}>{CATEGORY_LABELS[opp.categoryCode]}</span>
            <span className="chip bg-slate-100 text-slate-600">{WORKTYPE_LABELS[opp.workType]}</span>
            <span className="chip bg-slate-100 text-slate-600">{LOCATION_LABELS[opp.location]}</span>
            <span className={`chip ${STATUS_STYLES[opp.displayStatus]}`}>{STATUS_LABELS[opp.displayStatus]}</span>
          </div>

          {msg && <div className="mt-4 rounded-lg bg-brand-50 px-3 py-2 text-sm text-brand-700">{msg}</div>}

          <div className="mt-5 flex flex-wrap gap-3">
            {isExternal ? (
              <button onClick={external} className="btn-accent">Ứng tuyển tại trang chủ →</button>
            ) : (
              <button onClick={apply} className="btn-primary">Nộp hồ sơ nội bộ</button>
            )}
            <button onClick={toggleSave} className={saved ? 'btn-ghost bg-gold-50 text-gold-700' : 'btn-ghost'}>
              {saved ? '🔖 Đã lưu' : '🔖 Lưu'}
            </button>
            <button onClick={share} className="btn-ghost">↗ Chia sẻ</button>
          </div>
        </div>

        <Section title="Mô tả" html={opp.description} />
        <Section title="Yêu cầu" html={opp.requirements} />
        <Section title="Quyền lợi" html={opp.benefits} />
        {opp.salaryOrReward && <Section title="Lương / giải thưởng" text={opp.salaryOrReward} />}
        {opp.selectionProcess && <Section title="Quy trình tuyển chọn" text={opp.selectionProcess} />}
        {opp.applicationProcess && <Section title="Quy trình ứng tuyển" text={opp.applicationProcess} />}
      </div>

      <aside className="space-y-4">
        <div className="rounded-2xl border border-slate-100 bg-white p-5 shadow-card">
          <h3 className="mb-3 text-sm font-bold text-slate-700">Thông tin nhanh</h3>
          <Row label="Hạn nộp" value={fmtDate(opp.deadline)} />
          <Row label="Đăng bởi" value={opp.orgName} />
          {(opp.orgContactEmail || opp.orgWebsite) && (
            <>
              {opp.orgContactEmail && <Row label="Email" value={opp.orgContactEmail} />}
              {opp.orgContactPhone && <Row label="SĐT" value={opp.orgContactPhone} />}
              {opp.orgWebsite && <Row label="Website" value={opp.orgWebsite} />}
            </>
          )}
          <Row label="Lượt xem" value={opp.viewCount} />
          <Row label="Đã lưu" value={opp.bookmarkCount} />
          <Row label="Chia sẻ" value={opp.shareCount ?? 0} />
        </div>

        {related.length > 0 && (
          <div className="rounded-2xl border border-slate-100 bg-white p-5 shadow-card">
            <h3 className="mb-3 text-sm font-bold text-slate-700">Cơ hội liên quan</h3>
            <ul className="space-y-2 text-sm">
              {related.map((r) => (
                <li key={r.oppId}>
                  <Link to={`/opportunities/${r.slug || r.oppId}`} className="text-brand-600 hover:underline">{r.title}</Link>
                </li>
              ))}
            </ul>
          </div>
        )}
      </aside>
    </div>
  )
}

function Section({ title, html, text }) {
  return (
    <div className="rounded-2xl border border-slate-100 bg-white p-6 shadow-card">
      <h3 className="mb-2 text-sm font-bold text-slate-700">{title}</h3>
      {html ? <div className="prose-sm text-sm leading-relaxed text-slate-600" dangerouslySetInnerHTML={{ __html: html }} />
        : <p className="text-sm text-slate-600">{text}</p>}
    </div>
  )
}
function Row({ label, value }) {
  return (
    <div className="flex justify-between border-b border-slate-50 py-2 text-sm last:border-0">
      <span className="text-slate-400">{label}</span>
      <span className="font-medium text-slate-700">{value}</span>
    </div>
  )
}
