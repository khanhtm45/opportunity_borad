import { useEffect, useState } from 'react'
import { useParams, useNavigate, Link } from 'react-router-dom'
import api from '../api/client.js'
import { studentApi } from '../api/student.js'
import { useAuth } from '../context/AuthContext.jsx'
import {
  CATEGORY_LABELS, CATEGORY_STYLES, STATUS_LABELS, STATUS_STYLES,
  WORKTYPE_LABELS, LOCATION_LABELS, EMPLOYMENT_TYPE_LABELS, JOB_LEVEL_LABELS,
  EXPERIENCE_LEVEL_LABELS, EDUCATION_LEVEL_LABELS, COMPANY_SIZE_LABELS,
  fmtDate, fmtDateTime,
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
      const profile = await studentApi.profile().catch(() => null)
      if (!profile?.hasCv) {
        setMsg('⚠️ Cần tải CV lên hồ sơ trước khi nộp đơn')
        navigate('/me/profile')
        return
      }
      await studentApi.apply(opp.oppId)
      setMsg('✅ Đã nộp hồ sơ thành công (kèm CV từ hồ sơ)!')
    } catch (e) {
      const m = e.response?.data?.error?.message || 'Lỗi khi nộp'
      setMsg(m)
      if (m.toLowerCase().includes('cv')) navigate('/me/profile')
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
            {opp.employmentType && <span className="chip bg-slate-100 text-slate-600">{EMPLOYMENT_TYPE_LABELS[opp.employmentType] || opp.employmentType}</span>}
            {opp.jobLevel && <span className="chip bg-slate-100 text-slate-600">{JOB_LEVEL_LABELS[opp.jobLevel] || opp.jobLevel}</span>}
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
              <svg className="h-4 w-4" viewBox="0 0 24 24" fill={saved ? 'currentColor' : 'none'} stroke="currentColor" strokeWidth="1.6">
                <path strokeLinecap="round" strokeLinejoin="round" d="M17.593 3.322c1.1.128 1.907 1.077 1.907 2.185V21L12 17.25 4.5 21V5.507c0-1.108.806-2.057 1.907-2.185a48.507 48.507 0 0111.186 0z" />
              </svg>
              {saved ? 'Đã lưu' : 'Lưu'}
            </button>
            <button onClick={share} className="btn-ghost">↗ Chia sẻ</button>
          </div>
        </div>

        <Section title="Mô tả" html={opp.description} />
        <Section title="Yêu cầu" html={opp.requirements} />
        <Section title="Quyền lợi" html={opp.benefits} />
        {opp.salaryOrReward && <Section title="Lương / giải thưởng" text={opp.salaryOrReward} />}
        {opp.workingSchedule && <Section title="Thời gian làm việc" text={opp.workingSchedule} />}
        {opp.selectionProcess && <Section title="Quy trình tuyển chọn" text={opp.selectionProcess} />}
        {opp.applicationProcess && <Section title="Quy trình ứng tuyển" text={opp.applicationProcess} />}
        {opp.skills && <Section title="Kỹ năng" text={opp.skills} />}
      </div>

      <aside className="space-y-4">
        <div className="rounded-2xl border border-slate-100 bg-white p-5 shadow-card">
          <h3 className="mb-3 text-sm font-bold text-slate-700">Thông tin nhanh</h3>
          <Row label="Hạn nộp" value={fmtDate(opp.deadline)} />
          <Row label="Đăng bởi" value={opp.orgName} />
          {opp.salaryNegotiable
            ? <Row label="Mức lương" value="Thỏa thuận" />
            : (opp.salaryMin != null || opp.salaryMax != null) && (
              <Row label="Mức lương" value={`${opp.salaryMin ?? '?'} – ${opp.salaryMax ?? '?'} ${opp.salaryCurrency || 'VND'}`} />
            )}
          {opp.headcount != null && <Row label="Số lượng" value={`${opp.headcount} người`} />}
          {opp.employmentType && <Row label="Loại hình" value={EMPLOYMENT_TYPE_LABELS[opp.employmentType] || opp.employmentType} />}
          {opp.jobLevel && <Row label="Cấp bậc" value={JOB_LEVEL_LABELS[opp.jobLevel] || opp.jobLevel} />}
          {opp.experienceLevel && <Row label="Kinh nghiệm" value={EXPERIENCE_LEVEL_LABELS[opp.experienceLevel] || opp.experienceLevel} />}
          {opp.educationLevel && <Row label="Học vấn" value={EDUCATION_LEVEL_LABELS[opp.educationLevel] || opp.educationLevel} />}
          {opp.addressDetail && <Row label="Địa chỉ" value={opp.addressDetail} />}
          {(opp.orgContactEmail || opp.orgWebsite || opp.orgTaxCode) && (
            <>
              {opp.orgContactEmail && <Row label="Email" value={opp.orgContactEmail} />}
              {opp.orgContactPhone && <Row label="SĐT" value={opp.orgContactPhone} />}
              {opp.orgWebsite && <Row label="Website" value={opp.orgWebsite} />}
              {opp.orgTaxCode && <Row label="MST" value={opp.orgTaxCode} />}
              {opp.orgAddress && <Row label="Địa chỉ CT" value={opp.orgAddress} />}
              {opp.orgIndustry && <Row label="Lĩnh vực" value={opp.orgIndustry} />}
              {opp.orgCompanySize && <Row label="Quy mô" value={COMPANY_SIZE_LABELS[opp.orgCompanySize] || opp.orgCompanySize} />}
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
  const ICONS = {
    'Mô tả': 'M19.5 14.25v-2.625a3.375 3.375 0 00-3.375-3.375h-1.5A1.125 1.125 0 0113.5 7.125v-1.5a3.375 3.375 0 00-3.375-3.375H8.25m2.25 0H5.625c-.621 0-1.125.504-1.125 1.125v17.25c0 .621.504 1.125 1.125 1.125h12.75c.621 0 1.125-.504 1.125-1.125V11.25a9 9 0 00-9-9z',
    'Yêu cầu': 'M15 10.5a3 3 0 11-6 0 3 3 0 016 0z M19.5 10.5a7.5 7.5 0 11-15 0 7.5 7.5 0 0115 0z M22.5 10.5a10.5 10.5 0 11-21 0 10.5 10.5 0 0121 0z',
    'Quyền lợi': 'M9 12.75L11.25 15 15 9.75M21 12c0 1.268-.63 2.39-1.593 3.068a3.745 3.745 0 01-1.043 3.296 3.745 3.745 0 01-3.296 1.043A3.745 3.745 0 0112 21c-1.268 0-2.39-.63-3.068-1.593a3.746 3.746 0 01-3.296-1.043 3.745 3.745 0 01-1.043-3.296A3.745 3.745 0 013 12c0-1.268.63-2.39 1.593-3.068a3.745 3.745 0 011.043-3.296 3.745 3.745 0 013.296-1.043A3.745 3.745 0 0112 3c1.268 0 2.39.63 3.068 1.593a3.745 3.745 0 013.296 1.043 3.745 3.745 0 011.043 3.296A3.745 3.745 0 0121 12z',
  }
  const d = ICONS[title]
  return (
    <div className="rounded-2xl border border-slate-100 bg-white p-6 shadow-card">
      <div className="mb-2 flex items-center gap-2">
        {d && (
          <span className="flex h-7 w-7 items-center justify-center rounded-lg bg-brand-50 text-brand-600">
            <svg className="h-4 w-4" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.6">
              <path strokeLinecap="round" strokeLinejoin="round" d={d} />
            </svg>
          </span>
        )}
        <h3 className="text-sm font-bold text-slate-700">{title}</h3>
      </div>
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
