import { useEffect, useState, useCallback, useMemo } from 'react'
import { adminApi } from '../api/admin.js'
import { useAuth } from '../context/AuthContext.jsx'
import { InlineLoader } from '../components/Splash.jsx'
import { APP_STATUS_LABELS, APP_STATUS_STYLES, OPP_STATUS_STYLES, STATUS_LABELS, fmtDate } from '../lib/constants.js'
import { asset } from '../lib/assets.js'

function StatCard({ label, value, hint, tone }) {
  const tones = {
    brand: 'from-brand-500 to-brand-600',
    accent: 'from-accent-500 to-accent-600',
    gold: 'from-amber-500 to-orange-500',
    emerald: 'from-emerald-500 to-teal-500',
  }
  return (
    <div className="relative overflow-hidden rounded-2xl border border-slate-100 bg-white p-5 shadow-card">
      <div className={`absolute -right-4 -top-4 h-20 w-20 rounded-full bg-gradient-to-br opacity-15 ${tones[tone] || tones.brand}`} />
      <p className="text-xs font-medium uppercase tracking-wide text-slate-400">{label}</p>
      <p className="mt-1 text-3xl font-extrabold text-slate-800">{value ?? 0}</p>
      {hint && <p className="mt-0.5 text-xs text-slate-400">{hint}</p>}
    </div>
  )
}

function BarChart({ title, items, color = '#0d9488' }) {
  const max = Math.max(1, ...items.map((i) => Number(i.count) || 0))
  return (
    <div className="rounded-2xl border border-slate-100 bg-white p-5 shadow-card">
      <h3 className="mb-4 text-sm font-bold text-slate-800">{title}</h3>
      {items.length === 0 ? (
        <p className="py-8 text-center text-sm text-slate-400">Chưa có dữ liệu</p>
      ) : (
        <div className="space-y-3">
          {items.map((it) => {
            const pct = Math.round(((Number(it.count) || 0) / max) * 100)
            return (
              <div key={it.label}>
                <div className="mb-1 flex justify-between text-xs">
                  <span className="font-medium text-slate-600">{it.label}</span>
                  <span className="text-slate-400">{it.count}</span>
                </div>
                <div className="h-2.5 overflow-hidden rounded-full bg-slate-100">
                  <div className="h-full rounded-full transition-all duration-500" style={{ width: `${pct}%`, background: color }} />
                </div>
              </div>
            )
          })}
        </div>
      )}
    </div>
  )
}

function DonutChart({ title, items, colors }) {
  const total = items.reduce((s, i) => s + (Number(i.count) || 0), 0) || 1
  let acc = 0
  const stops = items.map((it, idx) => {
    const start = (acc / total) * 100
    acc += Number(it.count) || 0
    const end = (acc / total) * 100
    return `${colors[idx % colors.length]} ${start}% ${end}%`
  })
  return (
    <div className="rounded-2xl border border-slate-100 bg-white p-5 shadow-card">
      <h3 className="mb-4 text-sm font-bold text-slate-800">{title}</h3>
      <div className="flex items-center gap-6">
        <div
          className="h-36 w-36 shrink-0 rounded-full"
          style={{ background: `conic-gradient(${stops.join(', ')})`, mask: 'radial-gradient(circle at center, transparent 52%, black 53%)', WebkitMask: 'radial-gradient(circle at center, transparent 52%, black 53%)' }}
        />
        <ul className="space-y-2 text-xs">
          {items.map((it, idx) => (
            <li key={it.label} className="flex items-center gap-2">
              <span className="h-2.5 w-2.5 rounded-full" style={{ background: colors[idx % colors.length] }} />
              <span className="text-slate-600">{it.label}</span>
              <span className="ml-auto font-semibold text-slate-800">{it.count}</span>
            </li>
          ))}
        </ul>
      </div>
    </div>
  )
}

function buildDefaultReason(scan) {
  if (!scan) return ''
  return scan.moderationNote || [
    scan.summary,
    ...(scan.risks || []),
    ...(scan.contentMismatches || []).map((x) => `Không khớp: ${x}`),
    ...(scan.recommendations || []).map((x) => `Cần: ${x}`),
  ].filter(Boolean).join(' | ')
}

export default function AdminPage() {
  const { user } = useAuth()
  const [tab, setTab] = useState('dashboard')
  const [queue, setQueue] = useState([])
  const [users, setUsers] = useState([])
  const [analytics, setAnalytics] = useState(null)
  const [loading, setLoading] = useState(true)
  const [msg, setMsg] = useState('')
  const [scanResult, setScanResult] = useState(null)
  const [oppScanResult, setOppScanResult] = useState(null)
  const [reviewReason, setReviewReason] = useState('')
  const [scanningId, setScanningId] = useState(null)
  const [scanningOppId, setScanningOppId] = useState(null)
  const [apps, setApps] = useState([])
  const [appCriteria, setAppCriteria] = useState('')
  const [appScanOppId, setAppScanOppId] = useState('')
  const [scanningApps, setScanningApps] = useState(false)
  const [appScanBatch, setAppScanBatch] = useState(null)
  const [appReasons, setAppReasons] = useState({})

  const loadQueue = useCallback(() => adminApi.moderationQueue().then((r) => setQueue(r || [])), [])
  const loadUsers = useCallback(() => adminApi.users().then((r) => setUsers(r || [])), [])
  const loadAnalytics = useCallback(() => adminApi.analytics().then((r) => setAnalytics(r)).catch(() => null), [])
  const loadApps = useCallback(() => adminApi.applications().then((r) => setApps(r || [])).catch(() => setApps([])), [])

  const refresh = useCallback(() => {
    setLoading(true)
    Promise.all([loadQueue(), loadUsers(), loadAnalytics(), loadApps()]).finally(() => setLoading(false))
  }, [loadQueue, loadUsers, loadAnalytics, loadApps])

  useEffect(() => { if (user?.role === 'ADMIN') refresh(); else setLoading(false) }, [user, refresh])

  const act = async (id, action) => {
    try {
      if (action === 'approve') await adminApi.approve(id)
      if (action === 'reject') {
        const r = prompt('Lý do từ chối:', reviewReason || '')
        if (!r) return
        await adminApi.reject(id, r)
      }
      if (action === 'feature') await adminApi.feature(id)
      setMsg(`✅ Đã ${action === 'approve' ? 'duyệt' : action === 'reject' ? 'từ chối' : 'Featured'}`)
      setOppScanResult(null)
      refresh()
    } catch (e) { setMsg(e.response?.data?.error?.message || 'Lỗi') }
  }

  const verifyOrg = async (userId) => { await adminApi.verifyOrg(userId); setMsg('✅ Đã duyệt tổ chức'); refresh() }

  const aiScanOrg = async (userId) => {
    setScanningId(userId); setMsg(''); setScanResult(null); setOppScanResult(null)
    try {
      const r = await adminApi.aiScanOrgByUser(userId, true)
      setScanResult(r)
      const pct = Math.round((r.confidence || 0) * 100)
      if (r.appliedAction === 'VERIFIED') setMsg(`✅ Org VERIFIED (${pct}%)`)
      else if (r.appliedAction === 'NEEDS_UPDATE') setMsg(`⚠️ Org cần cập nhật (${pct}%)`)
      else setMsg(`ℹ️ Org scan: ${r.verdict} (${pct}%)`)
      refresh()
    } catch (e) {
      setMsg(e.response?.data?.error?.message || 'AI quét org lỗi')
    } finally { setScanningId(null) }
  }

  /** AI scan tin — lưu note, Admin xem lại rồi quyết định */
  const aiScanOpp = async (oppId) => {
    setScanningOppId(oppId); setMsg(''); setOppScanResult(null); setScanResult(null)
    try {
      const r = await adminApi.aiScanOpportunity(oppId, true)
      setOppScanResult(r)
      setReviewReason(buildDefaultReason(r))
      const pct = Math.round((r.confidence || 0) * 100)
      setMsg(`🤖 AI quét xong (${r.verdict}, ${pct}%) — xem lý do bên dưới, Admin xác nhận lại`)
      refresh()
    } catch (e) {
      setMsg(e.response?.data?.error?.message || 'AI quét tin lỗi — tin cần có hồ sơ chương trình')
    } finally { setScanningOppId(null) }
  }

  const sendRequestUpdate = async (oppId) => {
    const reason = (reviewReason || '').trim()
    if (!reason) { setMsg('⚠️ Nhập / giữ lý do trước khi gửi yêu cầu cập nhật'); return }
    try {
      await adminApi.requestUpdate(oppId, reason)
      setMsg('✅ Đã gửi yêu cầu cập nhật cho nhà đăng tin (tin về DRAFT)')
      setOppScanResult(null)
      refresh()
    } catch (e) { setMsg(e.response?.data?.error?.message || 'Lỗi gửi yêu cầu') }
  }

  const runAdminAppScan = async () => {
    const oppId = (appScanOppId || '').trim()
    const criteria = (appCriteria || '').trim()
    if (!oppId) { setMsg('⚠️ Chọn / dán oppId tin cần quét hồ sơ SV'); return }
    if (criteria.length < 10) { setMsg('⚠️ Nhập tiêu chuẩn screening (≥10 ký tự)'); return }
    setScanningApps(true)
    setAppScanBatch(null)
    setMsg('')
    try {
      const batch = await adminApi.aiScanApps(oppId, criteria, true)
      setAppScanBatch(batch)
      const reasons = {}
      ;(batch.results || []).forEach((r) => { reasons[r.appId] = r.moderationNote || r.summary || '' })
      setAppReasons(reasons)
      setMsg(`🤖 Đã quét ${batch.scannedCount || 0} hồ sơ SV — Admin xem lại nhóm lý do rồi gửi SV`)
      loadApps()
    } catch (e) {
      setMsg(e.response?.data?.error?.message || 'AI quét hồ sơ SV lỗi')
    } finally { setScanningApps(false) }
  }

  const sendAppUpdate = async (appId) => {
    const reason = (appReasons[appId] || '').trim()
    if (!reason) { setMsg('⚠️ Nhập lý do trước khi gửi SV'); return }
    try {
      await adminApi.requestAppUpdate(appId, reason)
      setMsg('✅ Đã gửi yêu cầu cập nhật hồ sơ cho sinh viên')
      loadApps()
    } catch (e) { setMsg(e.response?.data?.error?.message || 'Lỗi gửi yêu cầu') }
  }

  const a = analytics || {}
  const pendingCount = Number(a.pending ?? queue.length) || 0

  const oppBars = useMemo(() => (
    (a.oppByStatus || []).map((x) => ({
      label: STATUS_LABELS[x.status] || x.status,
      count: x.count,
    }))
  ), [a.oppByStatus])

  const roleDonut = useMemo(() => (
    (a.usersByRole || [
      { role: 'STUDENT', count: a.students || 0 },
      { role: 'PROVIDER', count: a.providers || 0 },
    ]).map((x) => ({ label: x.role, count: x.count }))
  ), [a])

  if (user?.role !== 'ADMIN')
    return <div className="py-16 text-center text-slate-400">Trang dành cho Quản trị viên.</div>

  const menu = [
    ['dashboard', 'Dashboard'],
    ['queue', 'Kiểm duyệt tin'],
    ['apps', 'Hồ sơ SV'],
    ['users', 'Người dùng'],
  ]

  const oppOptions = useMemo(() => {
    const map = new Map()
    apps.forEach((a) => {
      if (a.oppId && !map.has(a.oppId)) map.set(a.oppId, a.title || a.oppId)
    })
    return [...map.entries()]
  }, [apps])

  return (
    <div className="flex gap-6">
      <aside className="hidden w-56 shrink-0 md:block">
        <div className="sticky top-20 space-y-1">
          <div className="mb-4 rounded-2xl bg-slate-900 bg-cover bg-center px-4 py-4 text-white shadow-card" style={{ backgroundImage: `url(${asset('ob-network.svg')})` }}>
            <p className="text-[10px] uppercase tracking-widest text-white/70">Opportunity Board</p>
            <span className="text-lg font-extrabold drop-shadow">Admin</span>
          </div>
          {menu.map(([k, v]) => (
            <button key={k} type="button" onClick={() => setTab(k)}
              className={`flex w-full items-center justify-between rounded-xl px-4 py-2.5 text-sm font-medium transition ${
                tab === k ? 'bg-brand-500 text-white shadow-card' : 'text-slate-600 hover:bg-slate-100'}`}>
              <span>{v}</span>
              {k === 'queue' && pendingCount > 0 && (
                <span className="rounded-full bg-accent-500 px-2 py-0.5 text-xs text-white">{pendingCount}</span>
              )}
              {k === 'apps' && apps.length > 0 && (
                <span className="rounded-full bg-slate-200 px-2 py-0.5 text-xs text-slate-700">{apps.length}</span>
              )}
            </button>
          ))}
        </div>
      </aside>

      <div className="min-w-0 flex-1 space-y-4">
        <div className="flex flex-wrap items-center justify-between gap-3">
          <div>
            <h1 className="text-xl font-bold text-slate-800">Quản trị hệ thống</h1>
            <p className="text-xs text-slate-400">Xin chào, {user?.fullName || user?.email}</p>
          </div>
          {msg && <div className="max-w-xl rounded-lg bg-emerald-50 px-3 py-2 text-sm font-medium text-emerald-800">{msg}</div>}
        </div>

        <div className="flex gap-2 md:hidden">
          {menu.map(([k, v]) => (
            <button key={k} type="button" onClick={() => setTab(k)}
              className={`flex-1 rounded-xl px-3 py-2 text-xs font-medium ${tab === k ? 'bg-brand-500 text-white' : 'border border-slate-200 bg-white text-slate-600'}`}>
              {v}
            </button>
          ))}
        </div>

        {loading ? <InlineLoader /> : (
          <>
            {tab === 'dashboard' && (
              <div className="space-y-4">
                <div className="overflow-hidden rounded-3xl bg-gradient-to-br from-slate-900 via-slate-800 to-brand-900 p-6 text-white shadow-card">
                  <p className="text-xs uppercase tracking-widest text-white/60">Tổng quan vận hành</p>
                  <h2 className="mt-1 text-2xl font-extrabold">Dashboard kiểm duyệt</h2>
                  <p className="mt-2 max-w-xl text-sm text-white/75">
                    Theo dõi tin chờ duyệt, người dùng và kết quả AI — quét tin trước khi duyệt hoặc gửi yêu cầu cập nhật.
                  </p>
                  <button type="button" onClick={() => setTab('queue')} className="mt-4 rounded-xl bg-white/15 px-4 py-2 text-sm font-semibold backdrop-blur hover:bg-white/25">
                    Vào hàng đợi ({pendingCount})
                  </button>
                </div>

                <div className="grid grid-cols-2 gap-3 lg:grid-cols-4">
                  <StatCard label="Người dùng" value={a.users} hint={`${a.students ?? 0} SV · ${a.providers ?? 0} NTD`} tone="brand" />
                  <StatCard label="Cơ hội" value={a.opportunities} hint={`${a.approved ?? 0} đã duyệt`} tone="accent" />
                  <StatCard label="Đơn ứng tuyển" value={a.applications} hint="tổng đơn" tone="gold" />
                  <StatCard label="Chờ duyệt" value={pendingCount} hint="cần xử lý" tone="emerald" />
                </div>

                <div className="grid grid-cols-1 gap-4 lg:grid-cols-2">
                  <BarChart title="Tin theo trạng thái" items={oppBars} color="#0f766e" />
                  <DonutChart
                    title="Người dùng theo vai trò"
                    items={roleDonut}
                    colors={['#0ea5e9', '#f59e0b', '#64748b']}
                  />
                </div>

                <div className="grid grid-cols-1 gap-4 lg:grid-cols-3">
                  <div className="rounded-2xl border border-slate-100 bg-white p-5 shadow-card lg:col-span-1">
                    <h3 className="mb-3 text-sm font-bold text-slate-800">Tổ chức / thuế</h3>
                    <div className="space-y-2 text-sm">
                      <div className="flex justify-between"><span className="text-slate-500">Tổng org</span><span className="font-bold">{a.orgs ?? 0}</span></div>
                      <div className="flex justify-between"><span className="text-slate-500">Đã VERIFIED</span><span className="font-bold text-emerald-600">{a.orgsVerified ?? 0}</span></div>
                      <div className="flex justify-between"><span className="text-slate-500">Cần cập nhật</span><span className="font-bold text-amber-600">{a.orgsNeedsUpdate ?? 0}</span></div>
                    </div>
                  </div>
                  <div className="rounded-2xl border border-slate-100 bg-white p-4 shadow-card lg:col-span-2">
                    <div className="mb-3 flex items-center justify-between">
                      <h3 className="text-sm font-bold text-slate-700">Tin chờ duyệt gần đây</h3>
                      <button type="button" onClick={() => setTab('queue')} className="text-xs text-brand-600 hover:underline">Xem tất cả</button>
                    </div>
                    {queue.length === 0 ? <p className="py-8 text-center text-sm text-slate-400">Không có tin chờ duyệt.</p> : (
                      <ul className="divide-y divide-slate-50">
                        {queue.slice(0, 5).map((o) => (
                          <li key={o.oppId} className="flex items-center justify-between gap-3 py-2.5">
                            <div className="min-w-0">
                              <p className="truncate text-sm font-medium text-slate-700">{o.title}</p>
                              <p className="text-xs text-slate-400">{o.orgName}</p>
                            </div>
                            <button type="button" className="chip-btn shrink-0 text-accent-700" onClick={() => { setTab('queue'); aiScanOpp(o.oppId) }}>
                              AI scan
                            </button>
                          </li>
                        ))}
                      </ul>
                    )}
                  </div>
                </div>
              </div>
            )}

            {tab === 'queue' && (
              <div className="space-y-4">
                <div className="rounded-2xl border border-brand-100 bg-brand-50/50 px-4 py-3 text-xs text-slate-600">
                  <strong className="text-brand-800">Quy trình:</strong> bấm <em>AI quét tin</em> → xem lý do → chỉnh sửa nếu cần →
                  <em> Gửi yêu cầu cập nhật</em> (provider sửa + gửi lại) hoặc <em>Duyệt</em> / <em>Từ chối</em> sau khi Admin check lại.
                </div>

                <p className="text-xs text-slate-400">{queue.length} tin PENDING</p>
                {queue.length === 0 ? (
                  <div className="rounded-2xl border border-dashed border-slate-200 py-16 text-center text-slate-400">Không có tin chờ duyệt.</div>
                ) : queue.map((o) => (
                  <div key={o.oppId} className={`rounded-2xl border bg-white p-4 shadow-card ${oppScanResult?.oppId === o.oppId ? 'border-accent-300 ring-2 ring-accent-100' : 'border-slate-100'}`}>
                    <div className="flex items-start justify-between gap-4">
                      <div>
                        <h3 className="font-bold text-slate-800">{o.title}</h3>
                        <p className="text-xs text-slate-400">{o.orgName} · {o.categoryCode} · {o.location}</p>
                        {o.aiModerationNote && (
                          <p className="mt-2 rounded-lg bg-amber-50 px-3 py-2 text-xs text-amber-900">
                            Ghi chú AI/Admin trước đó: {o.aiModerationNote}
                          </p>
                        )}
                      </div>
                      <span className={`chip ${OPP_STATUS_STYLES[o.status] || 'bg-amber-50 text-amber-700'}`}>{STATUS_LABELS[o.status] || o.status || 'PENDING'}</span>
                    </div>
                    <div className="mt-3 flex flex-wrap gap-2">
                      <button type="button" onClick={() => aiScanOpp(o.oppId)} disabled={scanningOppId === o.oppId}
                        className="btn-accent px-4 py-2 text-xs">
                        {scanningOppId === o.oppId ? 'Đang quét AI…' : 'AI quét tin'}
                      </button>
                      <button type="button" onClick={() => act(o.oppId, 'approve')} className="btn-primary px-4 py-2 text-xs">Duyệt</button>
                      <button type="button" onClick={() => act(o.oppId, 'reject')} className="btn-ghost px-4 py-2 text-xs text-rose-600">Từ chối</button>
                      <button type="button" onClick={() => act(o.oppId, 'feature')} className="btn-ghost px-4 py-2 text-xs text-gold-700">★ Featured</button>
                    </div>

                    {oppScanResult?.oppId === o.oppId && (
                      <div className="mt-4 space-y-3 rounded-xl border border-slate-100 bg-slate-50/80 p-4">
                        <div className="flex items-center justify-between">
                          <h4 className="text-sm font-bold text-slate-800">Kết quả AI — Admin xem lại</h4>
                          <button type="button" className="text-xs text-slate-400" onClick={() => setOppScanResult(null)}>Đóng</button>
                        </div>
                        <p className="text-sm">
                          <span className={`chip ${
                            oppScanResult.verdict === 'APPROVE' ? 'bg-emerald-50 text-emerald-700'
                              : oppScanResult.verdict === 'REJECT' ? 'bg-rose-50 text-rose-700'
                                : 'bg-amber-50 text-amber-700'
                          }`}>{oppScanResult.verdict}</span>
                          <span className="ml-2 text-xs text-slate-400">
                            {Math.round((oppScanResult.confidence || 0) * 100)}% · org {oppScanResult.orgVerifiedStatus} · next {oppScanResult.nextAction}
                          </span>
                        </p>
                        <p className="text-sm text-slate-600">{oppScanResult.summary}</p>
                        {!!oppScanResult.risks?.length && (
                          <div>
                            <p className="text-xs font-semibold text-rose-700">Rủi ro / lý do</p>
                            <ul className="mt-1 list-disc pl-5 text-xs text-rose-600">
                              {oppScanResult.risks.map((f, i) => <li key={i}>{f}</li>)}
                            </ul>
                          </div>
                        )}
                        {!!oppScanResult.contentMismatches?.length && (
                          <div>
                            <p className="text-xs font-semibold text-rose-700">Không khớp nội dung</p>
                            <ul className="mt-1 list-disc pl-5 text-xs text-rose-600">
                              {oppScanResult.contentMismatches.map((f, i) => <li key={i}>{f}</li>)}
                            </ul>
                          </div>
                        )}
                        {!!oppScanResult.recommendations?.length && (
                          <div>
                            <p className="text-xs font-semibold text-brand-700">Đề xuất cho nhà đăng tin</p>
                            <ul className="mt-1 list-disc pl-5 text-xs text-brand-700">
                              {oppScanResult.recommendations.map((f, i) => <li key={i}>{f}</li>)}
                            </ul>
                          </div>
                        )}
                        <label className="block text-xs font-semibold text-slate-700">
                          Lý do gửi nhà đăng tin (có thể sửa trước khi gửi)
                          <textarea
                            className="input-base mt-1 min-h-[5rem] text-sm"
                            value={reviewReason}
                            onChange={(e) => setReviewReason(e.target.value)}
                            placeholder="VD: Thiếu thư hợp tác / hồ sơ không khớp tiêu đề…"
                          />
                        </label>
                        <div className="flex flex-wrap gap-2">
                          <button type="button" className="btn-accent px-4 py-2 text-xs" onClick={() => sendRequestUpdate(o.oppId)}>
                            Gửi yêu cầu cập nhật
                          </button>
                          <button type="button" className="btn-primary px-4 py-2 text-xs" onClick={() => act(o.oppId, 'approve')}>
                            Xác nhận duyệt
                          </button>
                          <button type="button" className="btn-ghost px-4 py-2 text-xs text-rose-600" onClick={() => act(o.oppId, 'reject')}>
                            Từ chối (dùng lý do trên)
                          </button>
                        </div>
                      </div>
                    )}
                  </div>
                ))}
              </div>
            )}

            {tab === 'apps' && (
              <div className="space-y-4">
                <div className="rounded-2xl border border-brand-100 bg-brand-50/50 px-4 py-3 text-xs text-slate-600">
                  <strong className="text-brand-800">Lớp 3 — Hồ sơ SV:</strong> chọn tin → nhập tiêu chuẩn →
                  <em> AI quét</em> → xem nhóm lý do → chỉnh → <em>Gửi yêu cầu cập nhật</em> cho sinh viên (giống Provider).
                </div>
                <div className="rounded-2xl border border-slate-100 bg-white p-4 shadow-card space-y-3">
                  <label className="block text-xs font-semibold text-slate-700">
                    Tin đăng (oppId)
                    <select
                      className="input-base mt-1 text-sm"
                      value={appScanOppId}
                      onChange={(e) => {
                        setAppScanOppId(e.target.value)
                        const sample = apps.find((x) => x.oppId === e.target.value)
                        if (sample?.screeningCriteria) setAppCriteria(sample.screeningCriteria)
                      }}
                    >
                      <option value="">— Chọn tin có ứng tuyển —</option>
                      {oppOptions.map(([id, title]) => (
                        <option key={id} value={id}>{title}</option>
                      ))}
                    </select>
                  </label>
                  <label className="block text-xs font-semibold text-slate-700">
                    Tiêu chuẩn screening
                    <textarea
                      className="input-base mt-1 min-h-[4.5rem] text-sm"
                      value={appCriteria}
                      onChange={(e) => setAppCriteria(e.target.value)}
                      placeholder="VD: CNTT năm 3–4, GPA ≥ 3.0, React/Java, CV rõ ràng…"
                    />
                  </label>
                  <button type="button" className="btn-accent px-4 py-2 text-xs" disabled={scanningApps} onClick={runAdminAppScan}>
                    {scanningApps ? 'Đang quét AI…' : 'AI quét hồ sơ SV'}
                  </button>
                </div>

                {appScanBatch && (
                  <div className="space-y-3">
                    <p className="text-sm font-bold text-slate-800">
                      Kết quả — {appScanBatch.scannedCount} hồ sơ · Đạt {appScanBatch.approveGroup?.length || 0} ·
                      Xem lại {appScanBatch.reviewGroup?.length || 0} · Từ chối gợi ý {appScanBatch.rejectGroup?.length || 0}
                    </p>
                    {[
                      { key: 'approveGroup', label: 'ĐẠT', tone: 'border-emerald-200 bg-emerald-50/50' },
                      { key: 'reviewGroup', label: 'CẦN BỔ SUNG', tone: 'border-amber-200 bg-amber-50/50' },
                      { key: 'rejectGroup', label: 'GỢI Ý TỪ CHỐI', tone: 'border-rose-200 bg-rose-50/50' },
                    ].map((g) => {
                      const list = appScanBatch[g.key] || []
                      if (!list.length) return null
                      return (
                        <div key={g.key} className={`rounded-xl border p-3 ${g.tone}`}>
                          <h4 className="mb-2 text-sm font-bold">{g.label} · {list.length}</h4>
                          <div className="space-y-3">
                            {list.map((r) => (
                              <div key={r.appId} className="rounded-lg bg-white/90 p-3 border border-white shadow-sm">
                                <p className="text-sm font-semibold">{r.studentName || r.studentEmail}
                                  <span className="ml-2 text-xs font-normal text-slate-400">{r.verdict} · {Math.round((r.confidence || 0) * 100)}%</span>
                                </p>
                                <p className="mt-1 text-xs text-slate-600">{r.summary}</p>
                                <textarea
                                  className="input-base mt-2 min-h-[3rem] text-xs"
                                  value={appReasons[r.appId] || ''}
                                  onChange={(e) => setAppReasons((m) => ({ ...m, [r.appId]: e.target.value }))}
                                />
                                <button type="button" className="btn-accent mt-2 px-3 py-1.5 text-[11px]" onClick={() => sendAppUpdate(r.appId)}>
                                  Gửi yêu cầu cập nhật cho SV
                                </button>
                              </div>
                            ))}
                          </div>
                        </div>
                      )
                    })}
                  </div>
                )}

                <div className="rounded-2xl border border-slate-100 bg-white shadow-card">
                  <div className="border-b border-slate-100 px-4 py-3 flex justify-between">
                    <h3 className="text-sm font-bold text-slate-700">Đơn SUBMITTED / REVIEWING</h3>
                    <button type="button" className="text-xs text-brand-700" onClick={loadApps}>Làm mới</button>
                  </div>
                  <div className="divide-y divide-slate-50">
                    {apps.length === 0 ? (
                      <p className="py-10 text-center text-sm text-slate-400">Chưa có đơn chờ xử lý.</p>
                    ) : apps.map((a) => (
                      <div key={a.appId} className="px-4 py-3">
                        <div className="flex flex-wrap items-start justify-between gap-2">
                          <div>
                            <p className="text-sm font-semibold text-slate-800">{a.studentName || a.studentEmail}</p>
                            <p className="text-xs text-slate-400">{a.title} · {a.orgName} · {fmtDate(a.appliedAt)}</p>
                          </div>
                          <span className={`chip ${APP_STATUS_STYLES[a.status] || 'bg-slate-100'}`}>{APP_STATUS_LABELS[a.status] || a.status}</span>
                        </div>
                        {a.aiModerationNote && (
                          <p className="mt-2 rounded-lg bg-amber-50 px-3 py-2 text-xs text-amber-900 whitespace-pre-wrap">{a.aiModerationNote}</p>
                        )}
                        <button
                          type="button"
                          className="chip-btn mt-2 text-accent-700"
                          onClick={() => { setTab('apps'); setAppScanOppId(a.oppId); if (a.screeningCriteria) setAppCriteria(a.screeningCriteria) }}
                        >
                          Quét theo tin này
                        </button>
                      </div>
                    ))}
                  </div>
                </div>
              </div>
            )}

            {tab === 'users' && (
              <div className="overflow-hidden rounded-2xl border border-slate-100 bg-white shadow-card">
                <div className="flex items-center justify-between border-b border-slate-100 px-4 py-3">
                  <h3 className="text-sm font-bold text-slate-700">Người dùng</h3>
                  <span className="rounded-full bg-brand-500 px-2 py-0.5 text-xs text-white">{users.length}</span>
                </div>
                <div className="overflow-x-auto">
                  <table className="w-full text-sm">
                    <thead className="bg-slate-50 text-left text-xs uppercase text-slate-400">
                      <tr>
                        <th className="px-4 py-2">#</th>
                        <th className="px-4 py-2">Họ tên</th>
                        <th className="px-4 py-2">Email</th>
                        <th className="px-4 py-2">Vai trò</th>
                        <th className="px-4 py-2">Trạng thái</th>
                        <th className="px-4 py-2" />
                      </tr>
                    </thead>
                    <tbody className="divide-y divide-slate-50">
                      {users.map((u, i) => (
                        <tr key={u.userId} className="hover:bg-slate-50">
                          <td className="px-4 py-2 text-slate-400">{i + 1}</td>
                          <td className="px-4 py-2 font-medium text-slate-700">{u.fullName || '—'}</td>
                          <td className="px-4 py-2 text-slate-500">{u.email}</td>
                          <td className="px-4 py-2"><span className="chip bg-slate-100 text-slate-600">{u.role}</span></td>
                          <td className="px-4 py-2"><span className="chip bg-slate-100">{u.status}</span></td>
                          <td className="px-4 py-2 text-right">
                            {u.role === 'PROVIDER' && (
                              <div className="flex flex-wrap justify-end gap-1">
                                <button type="button" onClick={() => aiScanOrg(u.userId)} disabled={scanningId === u.userId} className="chip-btn text-accent-700">
                                  {scanningId === u.userId ? 'Đang quét…' : 'AI quét org/thuế'}
                                </button>
                                <button type="button" onClick={() => verifyOrg(u.userId)} className="chip-btn text-brand-700">Duyệt org</button>
                              </div>
                            )}
                          </td>
                        </tr>
                      ))}
                    </tbody>
                  </table>
                </div>
                {scanResult && (
                  <div className="border-t border-slate-100 p-4 text-sm">
                    <div className="mb-2 flex justify-between">
                      <h4 className="font-bold text-slate-800">AI org — {scanResult.orgName}</h4>
                      <button type="button" className="text-xs text-slate-400" onClick={() => setScanResult(null)}>Đóng</button>
                    </div>
                    <p className="text-slate-600">{scanResult.summary}</p>
                    {scanResult.verificationNote && (
                      <p className="mt-2 rounded-lg bg-amber-50 p-3 text-xs text-amber-900">{scanResult.verificationNote}</p>
                    )}
                  </div>
                )}
              </div>
            )}
          </>
        )}
      </div>
    </div>
  )
}
