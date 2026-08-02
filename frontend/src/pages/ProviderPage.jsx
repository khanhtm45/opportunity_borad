import { useEffect, useState, useCallback } from 'react'
import { useNavigate } from 'react-router-dom'
import { providerApi } from '../api/provider.js'
import api from '../api/client.js'
import { useAuth } from '../context/AuthContext.jsx'
import { InlineLoader } from '../components/Splash.jsx'
import {
  CATEGORY_LABELS, OPP_STATUS_STYLES, STATUS_LABELS,
  WORKTYPE_LABELS, LOCATION_LABELS, EMPLOYMENT_TYPE_LABELS,
  JOB_LEVEL_LABELS, EXPERIENCE_LEVEL_LABELS, EDUCATION_LEVEL_LABELS, fmtDate,
} from '../lib/constants.js'
import { asset } from '../lib/assets.js'
import FileUploadButton, { mediaSrc } from '../components/FileUploadButton.jsx'

const EMPTY_DOC = { docType: 'PROGRAM_PROOF', title: '', fileUrl: '' }

const EMPTY = {
  title: '', categoryCode: 'INTERNSHIP', workType: 'ONLINE', location: 'TOAN_QUOC',
  deadline: '', description: '', requirements: '', benefits: '',
  salaryOrReward: '', salaryMin: '', salaryMax: '', salaryCurrency: 'VND', salaryNegotiable: false,
  selectionProcess: '', applyMode: 'INTERNAL',
  jobLevel: 'INTERN', experienceLevel: 'NONE', educationLevel: 'UNIVERSITY',
  headcount: 1, employmentType: 'FULL_TIME',
  addressDetail: '', workingSchedule: '', skills: '',
  logoUrl: '', bannerUrl: '', externalLink: '', externalRef: '',
  documents: [{ ...EMPTY_DOC }],
}

const OPP_DOC_LABELS = {
  PROGRAM_PROOF: 'Chứng minh chương trình',
  PARTNERSHIP_LETTER: 'Thư hợp tác / ủy quyền',
  OTHER: 'Khác',
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
  const [screeningCriteria, setScreeningCriteria] = useState('')
  const [scanningApps, setScanningApps] = useState(false)
  const [appScanBatch, setAppScanBatch] = useState(null)
  const [appReasons, setAppReasons] = useState({}) // appId -> editable reason
  const [orgDocs, setOrgDocs] = useState([])
  const [orgProfile, setOrgProfile] = useState(null)
  const [orgDocForm, setOrgDocForm] = useState({ docType: 'BUSINESS_LICENSE', title: '', fileUrl: '' })
  const [orgEdit, setOrgEdit] = useState({
    orgName: '', taxCode: '', address: '', contactPhone: '', industry: '', logoUrl: '',
  })
  const [orgLogoPreview, setOrgLogoPreview] = useState('')

  const load = useCallback(() => {
    setLoading(true)
    Promise.all([
      providerApi.list(),
      api.get('/categories').then((r) => r.data).catch(() => []),
      providerApi.orgDocuments().catch(() => []),
      providerApi.orgProfile().catch(() => null),
    ])
      .then(([l, c, docs, profile]) => {
        const catArr = Array.isArray(c) ? c : (c?.items || [])
        setList(l || [])
        setCats(catArr)
        setOrgDocs(Array.isArray(docs) ? docs : [])
        setOrgProfile(profile)
        if (profile) {
          setOrgEdit({
            orgName: profile.orgName || '',
            taxCode: profile.taxCode || '',
            address: profile.address || '',
            contactPhone: profile.contactPhone || '',
            industry: profile.industry || '',
            logoUrl: profile.logoUrl || '',
          })
          setOrgLogoPreview(mediaSrc(profile.logoAccessUrl || profile.logoUrl || ''))
        }
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
      const documents = (form.documents || [])
        .map((d) => ({
          docType: d.docType || 'PROGRAM_PROOF',
          title: (d.title || '').trim(),
          fileUrl: (d.fileUrl || '').trim(),
        }))
        .filter((d) => d.title && d.fileUrl)
      if (documents.length < 1) {
        setMsg('⚠️ Cần ít nhất 1 hồ sơ liên quan (tiêu đề + URL)')
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
        salaryMin: form.salaryMin !== '' && form.salaryMin != null ? Number(form.salaryMin) : null,
        salaryMax: form.salaryMax !== '' && form.salaryMax != null ? Number(form.salaryMax) : null,
        salaryCurrency: form.salaryCurrency || 'VND',
        salaryNegotiable: !!form.salaryNegotiable,
        headcount: form.headcount ? Number(form.headcount) : null,
        documents,
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
      setForm({ ...EMPTY, documents: [{ ...EMPTY_DOC }] }); setEditing(null); load()
    } catch (e2) { setMsg(e2.response?.data?.error?.message || 'Lỗi') }
  }

  const setDoc = (idx, patch) => {
    setForm((f) => {
      const documents = [...(f.documents || [])]
      documents[idx] = { ...documents[idx], ...patch }
      return { ...f, documents }
    })
  }
  const addDoc = () => setForm((f) => ({ ...f, documents: [...(f.documents || []), { ...EMPTY_DOC }] }))
  const removeDoc = (idx) => setForm((f) => {
    const documents = (f.documents || []).filter((_, i) => i !== idx)
    return { ...f, documents: documents.length ? documents : [{ ...EMPTY_DOC }] }
  })

  const openEdit = async (o) => {
    setEditing(o)
    setMsg('')
    let documents = [{ ...EMPTY_DOC }]
    try {
      const docs = await providerApi.oppDocuments(o.oppId)
      if (docs?.length) {
        documents = docs.map((d) => ({
          docType: d.docType || 'PROGRAM_PROOF',
          title: d.title || '',
          fileUrl: d.fileUrl || '',
        }))
      }
    } catch { /* keep empty doc row */ }
    setForm({ ...EMPTY, ...o, deadline: (o.deadline || '').slice(0, 10), documents })
  }
  const doHide = async (o) => { await (o.status === 'HIDDEN' ? providerApi.show(o.oppId) : providerApi.hide(o.oppId)); load() }
  const doClose = async (o) => { await providerApi.close(o.oppId); load() }
  const doExport = async (oppId) => {
    const blob = await providerApi.exportCsv(oppId)
    const url = URL.createObjectURL(blob)
    const a = document.createElement('a'); a.href = url; a.download = `applications-${oppId}.csv`; a.click()
  }
  const openApps = async (o) => {
    const data = await providerApi.applications(o.oppId)
    const items = data || []
    setApps({ opp: o, items })
    setAppScanBatch(null)
    setAppReasons({})
    const prev = items.find((a) => a.screeningCriteria)?.screeningCriteria
    setScreeningCriteria(prev || o.requirements || '')
  }
  const reloadApps = async () => {
    if (!apps?.opp) return
    const items = await providerApi.applications(apps.opp.oppId)
    setApps((a) => ({ ...a, items: items || [] }))
  }
  const changeApp = async (appId, status, note) => {
    try {
      await providerApi.setAppStatus(appId, status, note)
      setMsg(`✅ Đã cập nhật trạng thái → ${STATUS_LABELS[status] || status}`)
      await reloadApps()
    } catch (e) {
      setMsg(e.response?.data?.error?.message || e.response?.data || 'Không đổi được trạng thái')
    }
  }
  const runAppAiScan = async () => {
    if (!apps?.opp) return
    const criteria = (screeningCriteria || '').trim()
    if (criteria.length < 10) {
      setMsg('⚠️ Nhập tiêu chuẩn screening (≥10 ký tự) trước khi quét AI')
      return
    }
    setScanningApps(true)
    setMsg('')
    setAppScanBatch(null)
    try {
      const batch = await providerApi.aiScanApps(apps.opp.oppId, criteria, true)
      setAppScanBatch(batch)
      const reasons = {}
      ;(batch.results || []).forEach((r) => {
        reasons[r.appId] = r.moderationNote || r.summary || ''
      })
      setAppReasons(reasons)
      setMsg(`🤖 Đã quét ${batch.scannedCount || 0} hồ sơ — xem nhóm lý do bên dưới, kiểm tra lại rồi gửi SV`)
      await reloadApps()
    } catch (e) {
      setMsg(e.response?.data?.error?.message || 'AI quét hồ sơ lỗi')
    } finally {
      setScanningApps(false)
    }
  }
  const sendAppUpdate = async (appId) => {
    const reason = (appReasons[appId] || '').trim()
    if (!reason) { setMsg('⚠️ Nhập / giữ lý do trước khi gửi yêu cầu cập nhật'); return }
    try {
      await providerApi.requestAppUpdate(appId, reason)
      setMsg('✅ Đã gửi yêu cầu cập nhật hồ sơ cho sinh viên')
      await reloadApps()
    } catch (e) {
      setMsg(e.response?.data?.error?.message || 'Lỗi gửi yêu cầu')
    }
  }
  const rejectAppWithReason = async (appId) => {
    const reason = (appReasons[appId] || '').trim()
    if (!reason) { setMsg('⚠️ Nhập lý do từ chối trước'); return }
    await changeApp(appId, 'REJECTED', reason)
  }

  if (user?.role !== 'PROVIDER')
    return <div className="py-16 text-center text-slate-400">Trang dành cho Nhà tuyển dụng.</div>

  return (
    <div className="space-y-6">
      {/* Header */}
      <div className="overflow-hidden rounded-3xl bg-slate-900 bg-cover bg-center p-6 text-white shadow-card" style={{ backgroundImage: `url(${asset('ob-network.svg')})` }}>
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

      {orgProfile?.needsUpdate && (
        <div className="rounded-2xl border border-amber-200 bg-amber-50 p-4 text-amber-950 shadow-card">
          <p className="font-bold">Cần cập nhật hồ sơ tổ chức</p>
          <p className="mt-1 text-sm">{orgProfile.updateHint}</p>
          {orgProfile.verificationNote && (
            <p className="mt-2 text-sm whitespace-pre-wrap">{orgProfile.verificationNote}</p>
          )}
          <p className="mt-2 text-xs text-amber-800">
            Sửa thông tin bên dưới hoặc thêm giấy tờ mới → trạng thái về chờ duyệt lại. Liên hệ Admin nếu cần hỗ trợ.
          </p>
        </div>
      )}

      <div className="rounded-2xl border border-slate-100 bg-white p-5 shadow-card">
        <h2 className="mb-2 text-lg font-bold text-slate-800">Hồ sơ tổ chức</h2>
        <p className="mb-1 text-xs text-slate-400">
          Trạng thái: <span className="font-semibold text-slate-700">{orgProfile?.verifiedStatus || '—'}</span>
          {orgProfile && !orgProfile.needsUpdate && orgProfile.verifiedStatus === 'PENDING' && ' — đang chờ Admin/AI kiểm duyệt'}
          {orgProfile?.verifiedStatus === 'VERIFIED' && ' — đã xác minh'}
        </p>
        <p className="mb-3 text-xs text-slate-400">Cần ≥1 hồ sơ để Admin xác minh tổ chức trước khi đăng tin.</p>

        <div className="mb-4 grid gap-2 md:grid-cols-2">
          <div className="md:col-span-2 flex flex-wrap items-center gap-3 rounded-xl border border-slate-100 bg-slate-50/80 p-3">
            {orgLogoPreview ? (
              <img src={orgLogoPreview} alt="" className="h-14 w-14 rounded-xl object-cover border border-slate-200" />
            ) : (
              <div className="flex h-14 w-14 items-center justify-center rounded-xl bg-slate-200 text-xs text-slate-500">Avatar</div>
            )}
            <div className="min-w-0 flex-1">
              <p className="text-xs font-semibold text-slate-700">Logo / avatar tổ chức</p>
              <p className="text-[11px] text-slate-400">Upload S3 private + AES256 — không public bucket</p>
              <FileUploadButton
                purpose="avatar"
                accept="image/png,image/jpeg,image/webp"
                label="Chọn ảnh avatar"
                className="btn-ghost mt-1 text-xs"
                onUploaded={(up) => {
                  setOrgEdit((e) => ({ ...e, logoUrl: up.url }))
                  setOrgLogoPreview(mediaSrc(up.viewUrl))
                  setMsg('✅ Đã upload avatar (nhớ bấm Lưu thông tin tổ chức)')
                }}
              />
            </div>
          </div>
          <input className="input-base" placeholder="Tên tổ chức" value={orgEdit.orgName}
            onChange={(e) => setOrgEdit({ ...orgEdit, orgName: e.target.value })} />
          <input className="input-base" placeholder="Mã số thuế" value={orgEdit.taxCode}
            onChange={(e) => setOrgEdit({ ...orgEdit, taxCode: e.target.value })} />
          <input className="input-base md:col-span-2" placeholder="Địa chỉ" value={orgEdit.address}
            onChange={(e) => setOrgEdit({ ...orgEdit, address: e.target.value })} />
          <input className="input-base" placeholder="SĐT liên hệ" value={orgEdit.contactPhone}
            onChange={(e) => setOrgEdit({ ...orgEdit, contactPhone: e.target.value })} />
          <input className="input-base" placeholder="Ngành nghề" value={orgEdit.industry}
            onChange={(e) => setOrgEdit({ ...orgEdit, industry: e.target.value })} />
          <button type="button" className="btn-accent md:col-span-2" onClick={async () => {
            try {
              await providerApi.updateOrgProfile(orgEdit)
              setMsg('✅ Đã cập nhật thông tin tổ chức — chờ kiểm duyệt lại nếu trước đó bị yêu cầu sửa')
              load()
            } catch (e2) { setMsg(e2.response?.data?.error?.message || 'Lỗi cập nhật tổ chức') }
          }}>Lưu thông tin tổ chức</button>
        </div>

        <div className="mb-3 space-y-1">
          {orgDocs.map((d) => (
            <div key={d.docId} className="flex items-center justify-between rounded-lg border border-slate-100 px-3 py-2 text-sm">
              <a className="truncate text-brand-600 hover:underline" href={mediaSrc(d.accessUrl || d.fileUrl)} target="_blank" rel="noreferrer">{d.title} ({d.docType})</a>
              <button type="button" className="chip-btn" onClick={() => providerApi.deleteOrgDocument(d.docId).then(load).catch((e2) => setMsg(e2.response?.data?.error?.message || 'Không xoá được'))}>Xoá</button>
            </div>
          ))}
          {orgDocs.length === 0 && <p className="text-sm text-slate-400">Chưa có hồ sơ tổ chức.</p>}
        </div>
        <div className="grid gap-2 md:grid-cols-[10rem_1fr_1fr_auto_auto]">
          <select className="input-base" value={orgDocForm.docType} onChange={(e) => setOrgDocForm({ ...orgDocForm, docType: e.target.value })}>
            <option value="BUSINESS_LICENSE">Giấy phép KD</option>
            <option value="TAX_CODE">MST</option>
            <option value="IDENTITY">Định danh</option>
            <option value="OTHER">Khác</option>
          </select>
          <input className="input-base" placeholder="Tiêu đề" value={orgDocForm.title} onChange={(e) => setOrgDocForm({ ...orgDocForm, title: e.target.value })} />
          <input className="input-base" placeholder="ob-s3://… hoặc https://" value={orgDocForm.fileUrl} onChange={(e) => setOrgDocForm({ ...orgDocForm, fileUrl: e.target.value })} />
          <FileUploadButton
            purpose="org-doc"
            accept="image/*,.pdf,.doc,.docx"
            label="Upload file"
            onUploaded={(up) => {
              setOrgDocForm((f) => ({
                ...f,
                fileUrl: up.url,
                title: f.title || up.key?.split('/').pop() || 'Hồ sơ',
              }))
              setMsg('✅ Đã upload hồ sơ (S3 mã hóa) — bấm Thêm để lưu')
            }}
          />
          <button type="button" className="btn-primary" onClick={async () => {
            if (!orgDocForm.title.trim() || !orgDocForm.fileUrl.trim()) { setMsg('⚠️ Điền đủ tiêu đề + file/URL hồ sơ tổ chức'); return }
            try {
              await providerApi.addOrgDocument(orgDocForm)
              setOrgDocForm({ docType: 'BUSINESS_LICENSE', title: '', fileUrl: '' })
              setMsg('✅ Đã thêm hồ sơ — chờ kiểm duyệt lại')
              load()
            } catch (e2) { setMsg(e2.response?.data?.error?.message || 'Lỗi thêm hồ sơ') }
          }}>Thêm</button>
        </div>
      </div>

      <div className="grid gap-6 lg:grid-cols-2">
        {/* Form tạo/sửa */}
        <div>
          <h1 className="mb-3 text-xl font-bold text-slate-800">{editing ? 'Sửa tin' : 'Đăng tin cơ hội'}</h1>
          <form onSubmit={submitForm} className="space-y-3 rounded-2xl border border-slate-100 bg-white p-5 shadow-card">
            {msg && <div className="rounded-lg bg-brand-50 px-3 py-2 text-sm text-brand-700">{msg}</div>}
            <input className="input-base" placeholder="Tiêu đề" value={form.title} onChange={(e) => setForm({ ...form, title: e.target.value })} required />
            <div className="grid gap-2 sm:grid-cols-[1fr_auto]">
              <input className="input-base" placeholder="Logo tin (ob-s3://… — để trống dùng avatar org)" value={form.logoUrl || ''} onChange={(e) => setForm({ ...form, logoUrl: e.target.value })} />
              <FileUploadButton purpose="logo" accept="image/*" label="Upload logo" onUploaded={(up) => setForm((f) => ({ ...f, logoUrl: up.url }))} />
            </div>
            <div className="grid gap-2 sm:grid-cols-[1fr_auto]">
              <input className="input-base" placeholder="Banner tin (ob-s3://…)" value={form.bannerUrl || ''} onChange={(e) => setForm({ ...form, bannerUrl: e.target.value })} />
              <FileUploadButton purpose="banner" accept="image/*" label="Upload banner" onUploaded={(up) => setForm((f) => ({ ...f, bannerUrl: up.url }))} />
            </div>
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
              <select className="input-base" value={form.employmentType} onChange={(e) => setForm({ ...form, employmentType: e.target.value })}>
                {Object.entries(EMPLOYMENT_TYPE_LABELS).map(([k, v]) => <option key={k} value={k}>{v}</option>)}
              </select>
              <select className="input-base" value={form.jobLevel} onChange={(e) => setForm({ ...form, jobLevel: e.target.value })}>
                {Object.entries(JOB_LEVEL_LABELS).map(([k, v]) => <option key={k} value={k}>{v}</option>)}
              </select>
              <select className="input-base" value={form.experienceLevel} onChange={(e) => setForm({ ...form, experienceLevel: e.target.value })}>
                {Object.entries(EXPERIENCE_LEVEL_LABELS).map(([k, v]) => <option key={k} value={k}>{v}</option>)}
              </select>
              <select className="input-base" value={form.educationLevel} onChange={(e) => setForm({ ...form, educationLevel: e.target.value })}>
                {Object.entries(EDUCATION_LEVEL_LABELS).map(([k, v]) => <option key={k} value={k}>{v}</option>)}
              </select>
            </div>
            <div className="grid grid-cols-2 gap-3">
              <input className="input-base" type="number" min={1} placeholder="Số lượng tuyển" value={form.headcount} onChange={(e) => setForm({ ...form, headcount: e.target.value })} />
              <input className="input-base" type="date" value={form.deadline} onChange={(e) => setForm({ ...form, deadline: e.target.value })} required />
            </div>
            <input className="input-base" placeholder="Địa chỉ chi tiết (quận/huyện…)" value={form.addressDetail || ''} onChange={(e) => setForm({ ...form, addressDetail: e.target.value })} />
            <input className="input-base" placeholder="Kỹ năng (cách nhau bởi dấu phẩy)" value={form.skills || ''} onChange={(e) => setForm({ ...form, skills: e.target.value })} />
            <textarea className="input-base" rows={3} placeholder="Mô tả" value={form.description} onChange={(e) => setForm({ ...form, description: e.target.value })} />
            <textarea className="input-base" rows={2} placeholder="Yêu cầu" value={form.requirements} onChange={(e) => setForm({ ...form, requirements: e.target.value })} />
            <textarea className="input-base" rows={2} placeholder="Quyền lợi" value={form.benefits} onChange={(e) => setForm({ ...form, benefits: e.target.value })} />
            <div className="grid grid-cols-2 gap-3 md:grid-cols-4">
              <input className="input-base" type="number" min={0} placeholder="Lương min" value={form.salaryMin} onChange={(e) => setForm({ ...form, salaryMin: e.target.value })} />
              <input className="input-base" type="number" min={0} placeholder="Lương max" value={form.salaryMax} onChange={(e) => setForm({ ...form, salaryMax: e.target.value })} />
              <select className="input-base" value={form.salaryCurrency} onChange={(e) => setForm({ ...form, salaryCurrency: e.target.value })}>
                <option value="VND">VND</option>
                <option value="USD">USD</option>
              </select>
              <label className="flex items-center gap-2 text-sm text-slate-600">
                <input type="checkbox" checked={!!form.salaryNegotiable} onChange={(e) => setForm({ ...form, salaryNegotiable: e.target.checked })} />
                Thỏa thuận
              </label>
            </div>
            <textarea className="input-base" rows={2} placeholder="Lương / giải thưởng (mô tả thêm)" value={form.salaryOrReward || ''} onChange={(e) => setForm({ ...form, salaryOrReward: e.target.value })} />
            <textarea className="input-base" rows={2} placeholder="Thời gian làm việc (T2–T6…)" value={form.workingSchedule || ''} onChange={(e) => setForm({ ...form, workingSchedule: e.target.value })} />
            <textarea className="input-base" rows={2} placeholder="Quy trình tuyển chọn / lịch trình" value={form.selectionProcess || ''} onChange={(e) => setForm({ ...form, selectionProcess: e.target.value })} />
            {form.applyMode === 'EXTERNAL' && (
              <input className="input-base" placeholder="External link (https://...)" value={form.externalLink || ''} onChange={(e) => setForm({ ...form, externalLink: e.target.value })} />
            )}
            <input className="input-base" placeholder="Mã tham chiếu ngoài (case index, tùy chọn)" value={form.externalRef || ''} onChange={(e) => setForm({ ...form, externalRef: e.target.value })} />
            <div className="space-y-2 rounded-xl border border-slate-100 bg-slate-50/80 p-3">
              <div className="flex items-center justify-between">
                <p className="text-sm font-semibold text-slate-700">Hồ sơ liên quan <span className="font-normal text-slate-400">(bắt buộc ≥1)</span></p>
                <button type="button" className="chip-btn" onClick={addDoc}>+ Thêm</button>
              </div>
              {(form.documents || []).map((d, idx) => (
                <div key={idx} className="grid gap-2 rounded-lg border border-slate-100 bg-white p-2 sm:grid-cols-[9rem_1fr_1fr_auto_auto]">
                  <select className="input-base" value={d.docType} onChange={(e) => setDoc(idx, { docType: e.target.value })}>
                    {Object.entries(OPP_DOC_LABELS).map(([k, v]) => <option key={k} value={k}>{v}</option>)}
                  </select>
                  <input className="input-base" placeholder="Tiêu đề hồ sơ" value={d.title} onChange={(e) => setDoc(idx, { title: e.target.value })} required />
                  <input className="input-base" placeholder="ob-s3://… hoặc https://" value={d.fileUrl} onChange={(e) => setDoc(idx, { fileUrl: e.target.value })} required />
                  <FileUploadButton
                    purpose="opp-doc"
                    accept="image/*,.pdf,.doc,.docx"
                    label="Upload"
                    onUploaded={(up) => setDoc(idx, { fileUrl: up.url, title: d.title || up.key?.split('/').pop() || d.title })}
                  />
                  <button type="button" className="chip-btn" onClick={() => removeDoc(idx)} disabled={(form.documents || []).length <= 1}>Xoá</button>
                </div>
              ))}
            </div>
            <div className="flex gap-2">
              <button className="btn-primary flex-1">{editing ? 'Lưu' : 'Tạo & Gửi duyệt'}</button>
              {editing && <button type="button" className="btn-ghost" onClick={() => { setEditing(null); setForm({ ...EMPTY, documents: [{ ...EMPTY_DOC }] }) }}>Huỷ</button>}
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
                  {(o.aiModerationNote || (o.status === 'DRAFT' && o.rejectionReason)) && (
                    <p className="mt-2 rounded-lg bg-amber-50 px-2 py-1.5 text-xs text-amber-900">
                      Cần cập nhật: {o.aiModerationNote || o.rejectionReason}
                    </p>
                  )}
                  <p className="text-xs text-slate-400">Hạn {fmtDate(o.deadline)} · {o.applicationCount || 0} ứng tuyển{o.externalRef ? ` · Mã: ${o.externalRef}` : ''}</p>
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

      {/* Modal ứng tuyển + AI scan CV */}
      {apps && (
        <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/40 p-4" onClick={() => setApps(null)}>
          <div className="max-h-[90vh] w-full max-w-3xl overflow-auto rounded-2xl bg-white p-5 shadow-xl" onClick={(e) => e.stopPropagation()}>
            <div className="mb-3 flex items-center justify-between">
              <h2 className="text-lg font-bold">Ứng tuyển — {apps.opp.title}</h2>
              <button type="button" className="text-slate-400" onClick={() => setApps(null)}>✕</button>
            </div>

            <div className="mb-4 rounded-xl border border-brand-100 bg-brand-50/50 p-3 text-xs text-slate-600">
              <strong className="text-brand-800">Quy trình AI (giống Admin):</strong> nhập tiêu chuẩn →
              <em> AI quét hồ sơ</em> → xem nhóm lý do → chỉnh sửa → <em>Gửi yêu cầu cập nhật</em> cho SV
              hoặc Duyệt / Từ chối sau khi check lại.
            </div>

            <label className="mb-3 block text-xs font-semibold text-slate-700">
              Tiêu chuẩn screening (bắt buộc trước khi quét)
              <textarea
                className="input-base mt-1 min-h-[5rem] text-sm"
                value={screeningCriteria}
                onChange={(e) => setScreeningCriteria(e.target.value)}
                placeholder="VD: Sinh viên năm 3–4 CNTT, GPA ≥ 3.0, biết React/Java, có CV PDF rõ ràng, ưu tiên có dự án thực tế…"
              />
            </label>
            <div className="mb-4 flex flex-wrap gap-2">
              <button type="button" className="btn-accent px-4 py-2 text-xs" disabled={scanningApps} onClick={runAppAiScan}>
                {scanningApps ? 'Đang quét AI…' : 'AI quét hồ sơ SV'}
              </button>
              <button type="button" className="btn-ghost px-3 py-2 text-xs" onClick={reloadApps}>Làm mới danh sách</button>
            </div>

            {appScanBatch && (
              <div className="mb-5 space-y-4">
                <p className="text-sm font-bold text-slate-800">
                  Kết quả AI — {appScanBatch.scannedCount} hồ sơ
                  <span className="ml-2 text-xs font-normal text-slate-400">
                    (Đạt {appScanBatch.approveGroup?.length || 0} · Xem lại {appScanBatch.reviewGroup?.length || 0} · Từ chối gợi ý {appScanBatch.rejectGroup?.length || 0})
                  </span>
                </p>
                {[
                  { key: 'approveGroup', label: 'Nhóm ĐẠT (APPROVE)', tone: 'border-emerald-200 bg-emerald-50/60', chip: 'bg-emerald-50 text-emerald-700' },
                  { key: 'reviewGroup', label: 'Nhóm CẦN BỔ SUNG (REVIEW)', tone: 'border-amber-200 bg-amber-50/60', chip: 'bg-amber-50 text-amber-700' },
                  { key: 'rejectGroup', label: 'Nhóm GỢI Ý TỪ CHỐI (REJECT)', tone: 'border-rose-200 bg-rose-50/60', chip: 'bg-rose-50 text-rose-700' },
                ].map((g) => {
                  const list = appScanBatch[g.key] || []
                  if (!list.length) return null
                  return (
                    <div key={g.key} className={`rounded-xl border p-3 ${g.tone}`}>
                      <h3 className="mb-2 text-sm font-bold text-slate-800">{g.label} · {list.length}</h3>
                      <div className="space-y-3">
                        {list.map((r) => (
                          <div key={r.appId} className="rounded-lg border border-white/80 bg-white/90 p-3 shadow-sm">
                            <div className="flex flex-wrap items-start justify-between gap-2">
                              <div>
                                <p className="text-sm font-semibold">{r.studentName || r.studentEmail}</p>
                                <p className="text-xs text-slate-400">{r.studentEmail}</p>
                              </div>
                              <span className={`chip ${g.chip}`}>
                                {r.verdict} · {Math.round((r.confidence || 0) * 100)}%
                              </span>
                            </div>
                            <p className="mt-2 text-xs text-slate-600">{r.summary}</p>
                            {!!r.gaps?.length && (
                              <ul className="mt-1 list-disc pl-4 text-xs text-amber-800">
                                {r.gaps.map((x, i) => <li key={i}>Thiếu: {x}</li>)}
                              </ul>
                            )}
                            {!!r.risks?.length && (
                              <ul className="mt-1 list-disc pl-4 text-xs text-rose-700">
                                {r.risks.map((x, i) => <li key={i}>{x}</li>)}
                              </ul>
                            )}
                            {!!r.recommendations?.length && (
                              <ul className="mt-1 list-disc pl-4 text-xs text-brand-700">
                                {r.recommendations.map((x, i) => <li key={i}>Cần: {x}</li>)}
                              </ul>
                            )}
                            <label className="mt-2 block text-[11px] font-semibold text-slate-600">
                              Lý do gửi sinh viên (có thể sửa)
                              <textarea
                                className="input-base mt-1 min-h-[3.5rem] text-xs"
                                value={appReasons[r.appId] || ''}
                                onChange={(e) => setAppReasons((m) => ({ ...m, [r.appId]: e.target.value }))}
                              />
                            </label>
                            <div className="mt-2 flex flex-wrap gap-2">
                              <button type="button" className="btn-accent px-3 py-1.5 text-[11px]" onClick={() => sendAppUpdate(r.appId)}>
                                Gửi yêu cầu cập nhật
                              </button>
                              <button
                                type="button"
                                className="btn-primary px-3 py-1.5 text-[11px]"
                                onClick={async () => {
                                  if (r.appStatus === 'SUBMITTED') await changeApp(r.appId, 'REVIEWING')
                                  await changeApp(r.appId, 'INTERVIEW')
                                }}
                              >
                                Mời phỏng vấn
                              </button>
                              <button type="button" className="btn-ghost px-3 py-1.5 text-[11px] text-rose-600" onClick={() => rejectAppWithReason(r.appId)}>
                                Từ chối + gửi lý do
                              </button>
                            </div>
                          </div>
                        ))}
                      </div>
                    </div>
                  )
                })}
              </div>
            )}

            <h3 className="mb-2 text-sm font-bold text-slate-700">Danh sách ứng tuyển</h3>
            <div className="space-y-2">
              {apps.items.map((a) => (
                <div key={a.appId} className="rounded-lg border border-slate-100 p-3">
                  <div className="flex flex-wrap items-start justify-between gap-2">
                    <div className="text-sm">
                      <p className="font-medium">{a.studentName || a.studentEmail}</p>
                      <p className="text-xs text-slate-400">
                        {a.studentEmail} · Nộp {fmtDate(a.appliedAt)}
                        {a.university ? ` · ${a.university}` : ''}
                        {a.major ? ` · ${a.major}` : ''}
                      </p>
                    </div>
                    <select className="input-base w-40" value={a.status} onChange={(e) => changeApp(a.appId, e.target.value)}>
                      {['SUBMITTED', 'REVIEWING', 'INTERVIEW', 'ACCEPTED', 'REJECTED', 'WITHDRAWN'].map((s) => (
                        <option key={s} value={s}>{STATUS_LABELS[s] || s}</option>
                      ))}
                    </select>
                  </div>
                  {(a.aiModerationNote || a.providerNote || a.rejectionReason) && (
                    <p className="mt-2 rounded-lg bg-amber-50 px-3 py-2 text-xs text-amber-900 whitespace-pre-wrap">
                      {a.aiModerationNote || a.providerNote || a.rejectionReason}
                    </p>
                  )}
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
