import { useEffect, useState, useCallback } from 'react'
import { useNavigate } from 'react-router-dom'
import { providerApi } from '../api/provider.js'
import api from '../api/client.js'
import { useAuth } from '../context/AuthContext.jsx'
import { InlineLoader } from '../components/Splash.jsx'
import {
  CATEGORY_LABELS, OPP_STATUS_STYLES, STATUS_LABELS,
  WORKTYPE_LABELS, LOCATION_LABELS, fmtDate,
} from '../lib/constants.js'

const EMPTY = {
  title: '', categoryCode: 'INTERNSHIP', workType: 'ONLINE', location: 'TOAN_QUOC',
  deadline: '', description: '', requirements: '', benefits: '', applyMode: 'INTERNAL',
  logoUrl: '', externalLink: '',
}

export default function ProviderPage() {
  const { user } = useAuth()
  const navigate = useNavigate()
  const [list, setList] = useState([])
  const [stats, setStats] = useState(null)
  const [cats, setCats] = useState([])
  const [loading, setLoading] = useState(true)
  const [form, setForm] = useState(EMPTY)
  const [editing, setEditing] = useState(null) // opp đang sửa
  const [msg, setMsg] = useState('')
  const [apps, setApps] = useState(null) // modal danh sách ứng tuyển
  const [appStatus, setAppStatus] = useState({})

  const load = useCallback(() => {
    setLoading(true)
    Promise.all([
      providerApi.list(),
      api.get('/categories').then((r) => r.data).catch(() => []),
    ])
      .then(([l, c]) => {
        const catArr = Array.isArray(c) ? c : (c?.items || [])
        setList(l || [])
        setCats(catArr)
        const approved = l.filter((o) => o.status === 'APPROVED').length
        const pending = l.filter((o) => o.status === 'PENDING').length
        const apps = l.reduce((s, o) => s + (o.applicationCount || 0), 0)
        setStats({ total: l.length, approved, pending, applications: apps })
      })
      .finally(() => setLoading(false))
  }, [])

  useEffect(() => { if (user?.role === 'PROVIDER') load(); else setLoading(false) }, [user, load])

  const submitForm = async (e) => {
    e.preventDefault(); setMsg('')
    try {
      const cat = (cats || []).find((c) => c.code === form.categoryCode)
      if (!cat || !cat.categoryId) {
        setMsg('⚠️ Vui lòng chọn danh mục hợp lệ')
        return
      }
      let deadlineIso
      try { deadlineIso = new Date(form.deadline).toISOString() }
      catch { setMsg('⚠️ Ngày hết hạn không hợp lệ'); return }
      const body = {
        ...form,
        categoryId: cat.categoryId,
        logoUrl: form.logoUrl || undefined,
        deadline: deadlineIso,
      }
      if (editing) {
        await providerApi.update(editing.oppId, body)
        if (editing.status === 'DRAFT' || editing.status === 'HIDDEN')
          await providerApi.submit(editing.oppId)
        setMsg('✅ Đã cập nhật!')
      } else {
        const created = await providerApi.create(body)
        await providerApi.submit(created.oppId)
        setMsg('✅ Đã tạo và gửi duyệt!')
      }
      setForm(EMPTY); setEditing(null); load()
    } catch (e2) { setMsg(e2.response?.data?.error?.message || 'Lỗi') }
  }

  const openEdit = (o) => { setEditing(o); setForm({ ...EMPTY, ...o, deadline: (o.deadline || '').slice(0, 10) }); setMsg('') }
  const doHide = async (o) => { await (o.status === 'HIDDEN' ? providerApi.show(o.oppId) : providerApi.hide(o.oppId)); load() }
  const doClose = async (o) => { await providerApi.close(o.oppId); load() }
  const doExport = async (oppId) => {
    const blob = await providerApi.exportCsv(oppId)
    const url = URL.createObjectURL(blob)
    const a = document.createElement('a'); a.href = url; a.download = `applications-${oppId}.csv`; a.click()
  }
  const openApps = async (o) => {
    const data = await providerApi.applications(o.oppId)
    setApps({ opp: o, items: data || [] })
  }
  const changeApp = async (appId, status) => {
    await providerApi.setAppStatus(appId, status); setApps((a) => ({ ...a, items: a.items.map((x) => x.appId === appId ? { ...x, status } : x) }))
  }

  if (user?.role !== 'PROVIDER')
    return <div className="py-16 text-center text-slate-400">Trang dành cho Nhà tuyển dụng.</div>

  return (
    <div className="space-y-6">
      {/* Header */}
      <div className="overflow-hidden rounded-3xl bg-cover bg-center p-6 text-white shadow-card" style={{ backgroundImage: 'url(/network-bg.svg)' }}>
        <h1 className="text-xl font-extrabold drop-shadow md:text-2xl">🏢 Nhà tuyển dụng</h1>
        <p className="mt-1 text-sm text-white/85">Quản lý tin đăng, theo dõi ứng viên và thống kê.</p>
      </div>

      {/* Stats */}
      {stats && (
        <div className="grid grid-cols-2 gap-3 md:grid-cols-4">
          {[['Tổng tin', stats.total], ['Đang mở', stats.approved], ['Chờ duyệt', stats.pending], ['Ứng tuyển', stats.applications]].map(([k, v]) => (
            <div key={k} className="rounded-2xl border border-slate-100 bg-white p-4 shadow-card">
              <p className="text-2xl font-bold text-brand">{v ?? 0}</p>
              <p className="text-xs text-slate-400">{k}</p>
            </div>
          ))}
        </div>
      )}

      <div className="grid gap-6 lg:grid-cols-2">
        {/* Form tạo/sửa */}
        <div>
          <h1 className="mb-3 text-xl font-bold text-slate-800">{editing ? 'Sửa tin' : 'Đăng tin cơ hội'}</h1>
          <form onSubmit={submitForm} className="space-y-3 rounded-2xl border border-slate-100 bg-white p-5 shadow-card">
            {msg && <div className="rounded-lg bg-brand-50 px-3 py-2 text-sm text-brand-700">{msg}</div>}
            <input className="input-base" placeholder="Tiêu đề" value={form.title} onChange={(e) => setForm({ ...form, title: e.target.value })} required />
            <input className="input-base" placeholder="Logo URL (https://... để trống dùng logo tổ chức)" value={form.logoUrl || ''} onChange={(e) => setForm({ ...form, logoUrl: e.target.value })} />
            <div className="grid grid-cols-2 gap-3">
              <select className="input-base" value={form.categoryCode} onChange={(e) => setForm({ ...form, categoryCode: e.target.value })}>
                {Object.entries(CATEGORY_LABELS).map(([k, v]) => <option key={k} value={k}>{v}</option>)}
              </select>
              <select className="input-base" value={form.workType} onChange={(e) => setForm({ ...form, workType: e.target.value })}>
                {Object.entries(WORKTYPE_LABELS).map(([k, v]) => <option key={k} value={k}>{v}</option>)}
              </select>
              <select className="input-base" value={form.location} onChange={(e) => setForm({ ...form, location: e.target.value })}>
                {Object.entries(LOCATION_LABELS).map(([k, v]) => <option key={k} value={k}>{v}</option>)}
              </select>
              <select className="input-base" value={form.applyMode} onChange={(e) => setForm({ ...form, applyMode: e.target.value })}>
                <option value="INTERNAL">Nộp nội bộ</option>
                <option value="EXTERNAL">Link ngoài</option>
              </select>
            </div>
            <input className="input-base" type="date" value={form.deadline} onChange={(e) => setForm({ ...form, deadline: e.target.value })} required />
            <textarea className="input-base" rows={3} placeholder="Mô tả" value={form.description} onChange={(e) => setForm({ ...form, description: e.target.value })} />
            <textarea className="input-base" rows={2} placeholder="Yêu cầu" value={form.requirements} onChange={(e) => setForm({ ...form, requirements: e.target.value })} />
            <textarea className="input-base" rows={2} placeholder="Quyền lợi" value={form.benefits} onChange={(e) => setForm({ ...form, benefits: e.target.value })} />
            {form.applyMode === 'EXTERNAL' && (
              <input className="input-base" placeholder="External link (https://...)" value={form.externalLink || ''} onChange={(e) => setForm({ ...form, externalLink: e.target.value })} />
            )}
            <div className="flex gap-2">
              <button className="btn-primary flex-1">{editing ? 'Lưu' : 'Tạo & Gửi duyệt'}</button>
              {editing && <button type="button" className="btn-ghost" onClick={() => { setEditing(null); setForm(EMPTY) }}>Huỷ</button>}
            </div>
          </form>
        </div>

        {/* Danh sách */}
        <div>
          <h1 className="mb-3 text-xl font-bold text-slate-800">Tin đã đăng ({list.length})</h1>
          {loading ? <InlineLoader /> : (
            <div className="space-y-2">
              {list.map((o) => (
                <div key={o.oppId} className="rounded-xl border border-slate-100 bg-white p-3 shadow-card">
                  <div className="flex items-center justify-between">
                    <p className="text-sm font-semibold text-slate-700">{o.title}</p>
                    <span className={`chip ${OPP_STATUS_STYLES[o.status] || 'bg-slate-100'}`}>{STATUS_LABELS[o.status] || o.status}</span>
                  </div>
                  <p className="text-xs text-slate-400">Hạn {fmtDate(o.deadline)} · {o.applicationCount || 0} ứng tuyển</p>
                  <div className="mt-2 flex flex-wrap gap-1.5">
                    <button className="chip-btn" onClick={() => openEdit(o)}>Sửa</button>
                    {o.status === 'APPROVED' && <button className="chip-btn" onClick={() => doHide(o)}>Ẩn</button>}
                    {o.status === 'HIDDEN' && <button className="chip-btn" onClick={() => doHide(o)}>Hiện</button>}
                    {o.status === 'APPROVED' && <button className="chip-btn" onClick={() => doClose(o)}>Đóng</button>}
                    {o.status === 'DRAFT' && <button className="chip-btn" onClick={() => providerApi.submit(o.oppId).then(load)}>Gửi duyệt</button>}
                    <button className="chip-btn" onClick={() => openApps(o)}>Ứng tuyển</button>
                    <button className="chip-btn" onClick={() => doExport(o.oppId)}>Export CSV</button>
                  </div>
                </div>
              ))}
              {list.length === 0 && <p className="text-sm text-slate-400">Chưa có tin nào.</p>}
            </div>
          )}
        </div>
      </div>

      {/* Modal ứng tuyển */}
      {apps && (
        <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/40 p-4" onClick={() => setApps(null)}>
          <div className="max-h-[80vh] w-full max-w-2xl overflow-auto rounded-2xl bg-white p-5 shadow-xl" onClick={(e) => e.stopPropagation()}>
            <div className="mb-3 flex items-center justify-between">
              <h2 className="text-lg font-bold">Ứng tuyển — {apps.opp.title}</h2>
              <button className="text-slate-400" onClick={() => setApps(null)}>✕</button>
            </div>
            <div className="space-y-2">
              {apps.items.map((a) => (
                <div key={a.appId} className="flex items-center justify-between rounded-lg border border-slate-100 p-2">
                  <div className="text-sm">
                    <p className="font-medium">{a.studentName || a.studentEmail}</p>
                    <p className="text-xs text-slate-400">Nộp {fmtDate(a.appliedAt)}</p>
                  </div>
                  <select className="input-base w-40" value={a.status} onChange={(e) => changeApp(a.appId, e.target.value)}>
                    {['SUBMITTED', 'REVIEWING', 'INTERVIEW', 'ACCEPTED', 'REJECTED', 'WITHDRAWN'].map((s) => <option key={s} value={s}>{STATUS_LABELS[s] || s}</option>)}
                  </select>
                </div>
              ))}
              {apps.items.length === 0 && <p className="text-sm text-slate-400">Chưa có ứng tuyển.</p>}
            </div>
          </div>
        </div>
      )}
    </div>
  )
}
