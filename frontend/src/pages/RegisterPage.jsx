import { useState } from 'react'
import { Link } from 'react-router-dom'
import { useAuth } from '../context/AuthContext.jsx'
import { InlineLoader } from '../components/Splash.jsx'
import { AuthShell } from './LoginPage.jsx'
import { COMPANY_SIZE_LABELS } from '../lib/constants.js'
import FileUploadButton, { mediaSrc } from '../components/FileUploadButton.jsx'

const ROLES = [
  { v: 'STUDENT', l: 'Sinh viên', d: 'Tìm cơ hội, ứng tuyển, lưu tin' },
  { v: 'PROVIDER', l: 'Nhà tuyển dụng', d: 'Đăng tin (cần Admin duyệt org)' },
]

const ORG_DOC_LABELS = {
  BUSINESS_LICENSE: 'Giấy phép kinh doanh',
  TAX_CODE: 'Giấy tờ / chứng nhận MST',
  IDENTITY: 'Giấy tờ định danh người đại diện',
  OTHER: 'Khác',
}

const EMPTY_ORG_DOC = { docType: 'BUSINESS_LICENSE', title: '', fileUrl: '' }

export default function RegisterPage() {
  const { register } = useAuth()
  const [form, setForm] = useState({
    role: 'STUDENT',
    email: '',
    password: '',
    fullName: '',
    orgName: '',
    website: '',
    description: '',
    contactPhone: '',
    taxCode: '',
    address: '',
    industry: '',
    companySize: 'UNKNOWN',
    logoUrl: '',
    documents: [{ ...EMPTY_ORG_DOC }],
  })
  const [logoPreview, setLogoPreview] = useState('')
  const [err, setErr] = useState('')
  const [busy, setBusy] = useState(false)
  const [done, setDone] = useState(false)

  const set = (k) => (e) => setForm((f) => ({ ...f, [k]: e.target.value }))

  const setDoc = (idx, patch) => {
    setForm((f) => {
      const documents = [...(f.documents || [])]
      documents[idx] = { ...documents[idx], ...patch }
      return { ...f, documents }
    })
  }

  const addDoc = () => {
    setForm((f) => ({
      ...f,
      documents: [...(f.documents || []), { ...EMPTY_ORG_DOC, docType: 'TAX_CODE' }],
    }))
  }

  const removeDoc = (idx) => {
    setForm((f) => {
      const documents = (f.documents || []).filter((_, i) => i !== idx)
      return { ...f, documents: documents.length ? documents : [{ ...EMPTY_ORG_DOC }] }
    })
  }

  const submit = async (e) => {
    e.preventDefault()
    setBusy(true); setErr('')
    try {
      const payload = { ...form }
      if (form.role === 'PROVIDER') {
        if (!(form.orgName || '').trim()) {
          setErr('Vui lòng nhập tên tổ chức')
          setBusy(false)
          return
        }
        if (!(form.taxCode || '').trim()) {
          setErr('Nhà tuyển dụng cần mã số thuế (MST) để Admin/AI kiểm tra')
          setBusy(false)
          return
        }
        if (!(form.contactPhone || '').trim()) {
          setErr('Vui lòng nhập SĐT liên hệ tổ chức')
          setBusy(false)
          return
        }
        const documents = (form.documents || [])
          .map((d) => ({
            docType: d.docType || 'BUSINESS_LICENSE',
            title: (d.title || '').trim(),
            fileUrl: (d.fileUrl || '').trim(),
          }))
          .filter((d) => d.title && d.fileUrl)
        if (documents.length < 1) {
          setErr('Nhà tuyển dụng cần ít nhất 1 hồ sơ tổ chức (upload file hoặc dán URL)')
          setBusy(false)
          return
        }
        payload.documents = documents
        payload.orgName = form.orgName.trim()
        payload.taxCode = form.taxCode.trim()
        payload.contactPhone = form.contactPhone.trim()
        payload.website = (form.website || '').trim() || undefined
        payload.description = (form.description || '').trim() || undefined
        payload.address = (form.address || '').trim() || undefined
        payload.industry = (form.industry || '').trim() || undefined
        payload.logoUrl = (form.logoUrl || '').trim() || undefined
      }
      await register(payload)
      setDone(true)
    } catch (e2) {
      setErr(e2.response?.data?.error?.message || 'Đăng ký thất bại')
    } finally { setBusy(false) }
  }

  if (done) {
    const isProvider = form.role === 'PROVIDER'
    return (
      <AuthShell title="Đăng ký thành công" wide={isProvider}>
        <p className="text-center text-sm text-slate-600">
          {isProvider
            ? 'Tài khoản nhà tuyển dụng đã tạo. Tổ chức đang ở trạng thái chờ Admin duyệt — đăng nhập để bổ sung hồ sơ nếu cần.'
            : 'Chúng tôi đã ghi nhận tài khoản. Vui lòng xác thực email (nếu được yêu cầu) rồi đăng nhập.'}
        </p>
        <Link to="/login" className="btn-primary mt-4 w-full">Đến trang đăng nhập</Link>
      </AuthShell>
    )
  }

  const isProvider = form.role === 'PROVIDER'

  return (
    <AuthShell title="Tạo tài khoản" wide={isProvider}>
      <form onSubmit={submit} className="space-y-4">
        {err && <div className="rounded-lg bg-rose-50 px-3 py-2 text-sm text-rose-600">{err}</div>}

        <div className="grid grid-cols-2 gap-3">
          {ROLES.map((r) => (
            <button
              type="button"
              key={r.v}
              onClick={() => setForm((f) => ({ ...f, role: r.v }))}
              className={`rounded-xl border p-3 text-left text-xs transition ${
                form.role === r.v ? 'border-brand-500 bg-brand-50' : 'border-slate-200 hover:border-brand-300'
              }`}
            >
              <div className="font-bold text-slate-700">{r.l}</div>
              <div className="text-slate-400">{r.d}</div>
            </button>
          ))}
        </div>

        <label className="block text-xs font-semibold text-slate-600">
          Họ tên người liên hệ
          <input className="input-base mt-1" placeholder="Nguyễn Văn A" value={form.fullName} onChange={set('fullName')} required />
        </label>

        {isProvider && (
          <div className="space-y-3 rounded-xl border border-brand-100 bg-brand-50/40 p-3">
            <p className="text-sm font-bold text-brand-800">Thông tin tổ chức</p>
            <p className="text-[11px] text-slate-500">
              Cần đủ hồ sơ để Admin/AI duyệt. Chỉ đăng tin được sau khi tổ chức được xác minh.
            </p>

            <div className="flex flex-wrap items-center gap-3 rounded-xl border border-white/80 bg-white/90 p-3">
              {logoPreview ? (
                <img src={logoPreview} alt="" className="h-14 w-14 rounded-xl border border-slate-200 object-cover" />
              ) : (
                <div className="flex h-14 w-14 items-center justify-center rounded-xl bg-slate-100 text-[10px] text-slate-400">Logo</div>
              )}
              <div className="min-w-0 flex-1">
                <p className="text-xs font-semibold text-slate-700">Logo tổ chức (tuỳ chọn)</p>
                <FileUploadButton
                  guest
                  purpose="org-doc"
                  accept="image/png,image/jpeg,image/webp"
                  label="Upload logo"
                  className="btn-ghost mt-1 text-xs"
                  onUploaded={(up) => {
                    setForm((f) => ({ ...f, logoUrl: up.url }))
                    setLogoPreview(mediaSrc(up.viewUrl || up.url))
                  }}
                />
              </div>
            </div>

            <label className="block text-xs font-semibold text-slate-600">
              Tên tổ chức <span className="text-rose-500">*</span>
              <input className="input-base mt-1" placeholder="Công ty TNHH…" value={form.orgName} onChange={set('orgName')} required />
            </label>

            <div className="grid gap-3 sm:grid-cols-2">
              <label className="block text-xs font-semibold text-slate-600">
                Mã số thuế <span className="text-rose-500">*</span>
                <input className="input-base mt-1" placeholder="MST 10–13 số" value={form.taxCode} onChange={set('taxCode')} required />
              </label>
              <label className="block text-xs font-semibold text-slate-600">
                SĐT liên hệ <span className="text-rose-500">*</span>
                <input className="input-base mt-1" placeholder="09…" value={form.contactPhone} onChange={set('contactPhone')} required />
              </label>
            </div>

            <label className="block text-xs font-semibold text-slate-600">
              Địa chỉ công ty
              <input className="input-base mt-1" placeholder="Số nhà, quận/huyện, tỉnh/TP" value={form.address} onChange={set('address')} />
            </label>

            <div className="grid gap-3 sm:grid-cols-2">
              <label className="block text-xs font-semibold text-slate-600">
                Lĩnh vực hoạt động
                <input className="input-base mt-1" placeholder="CNTT, Giáo dục…" value={form.industry} onChange={set('industry')} />
              </label>
              <label className="block text-xs font-semibold text-slate-600">
                Quy mô nhân sự
                <select className="input-base mt-1" value={form.companySize} onChange={set('companySize')}>
                  {Object.entries(COMPANY_SIZE_LABELS).map(([k, v]) => (
                    <option key={k} value={k}>{v}</option>
                  ))}
                </select>
              </label>
            </div>

            <label className="block text-xs font-semibold text-slate-600">
              Website
              <input className="input-base mt-1" type="url" placeholder="https://…" value={form.website} onChange={set('website')} />
            </label>

            <label className="block text-xs font-semibold text-slate-600">
              Mô tả tổ chức
              <textarea
                className="input-base mt-1 min-h-[4.5rem]"
                placeholder="Ngắn gọn về công ty, sản phẩm, đối tượng tuyển…"
                value={form.description}
                onChange={set('description')}
              />
            </label>
          </div>
        )}

        <label className="block text-xs font-semibold text-slate-600">
          Email đăng nhập <span className="text-rose-500">*</span>
          <input className="input-base mt-1" type="email" placeholder="you@company.com" value={form.email} onChange={set('email')} required />
        </label>
        <label className="block text-xs font-semibold text-slate-600">
          Mật khẩu <span className="text-rose-500">*</span>
          <input className="input-base mt-1" type="password" placeholder="≥ 8 ký tự" value={form.password} onChange={set('password')} required minLength={8} />
        </label>

        {isProvider && (
          <div className="space-y-3 rounded-xl border border-slate-200 bg-slate-50/80 p-3">
            <div className="flex flex-wrap items-center justify-between gap-2">
              <div>
                <p className="text-sm font-semibold text-slate-700">
                  Hồ sơ tổ chức <span className="font-normal text-rose-500">*</span>
                </p>
                <p className="text-[11px] text-slate-400">Bắt buộc ≥1 file (GPKD / MST / định danh). Upload S3 private + AES.</p>
              </div>
              <button type="button" className="chip-btn" onClick={addDoc}>+ Thêm hồ sơ</button>
            </div>

            {(form.documents || []).map((d, idx) => (
              <div key={idx} className="space-y-2 rounded-xl border border-white bg-white p-3 shadow-sm">
                <div className="flex items-center justify-between gap-2">
                  <span className="text-xs font-semibold text-slate-500">Hồ sơ #{idx + 1}</span>
                  {(form.documents || []).length > 1 && (
                    <button type="button" className="text-xs text-rose-600" onClick={() => removeDoc(idx)}>Xoá</button>
                  )}
                </div>
                <select className="input-base" value={d.docType} onChange={(e) => setDoc(idx, { docType: e.target.value })}>
                  {Object.entries(ORG_DOC_LABELS).map(([k, v]) => (
                    <option key={k} value={k}>{v}</option>
                  ))}
                </select>
                <input
                  className="input-base"
                  placeholder="Tiêu đề hồ sơ (VD: GPKD 2024)"
                  value={d.title}
                  onChange={(e) => setDoc(idx, { title: e.target.value })}
                  required
                />
                <div className="flex flex-wrap gap-2">
                  <input
                    className="input-base min-w-0 flex-1"
                    placeholder="ob-s3://… sau khi upload hoặc https://"
                    value={d.fileUrl}
                    onChange={(e) => setDoc(idx, { fileUrl: e.target.value })}
                    required
                  />
                  <FileUploadButton
                    guest
                    purpose="org-doc"
                    accept="image/*,.pdf,.doc,.docx"
                    label="Upload file"
                    className="btn-primary text-xs"
                    onUploaded={(up) => setDoc(idx, {
                      fileUrl: up.url,
                      title: d.title || up.key?.split('/').pop() || 'Hồ sơ tổ chức',
                    })}
                  />
                </div>
                {d.fileUrl && (
                  <p className="truncate text-[11px] text-emerald-700">Đã gắn: {d.fileUrl}</p>
                )}
              </div>
            ))}
          </div>
        )}

        <button className="btn-primary w-full" disabled={busy}>
          {busy ? <InlineLoader /> : (isProvider ? 'Đăng ký nhà tuyển dụng' : 'Đăng ký')}
        </button>
      </form>
      <p className="mt-4 text-center text-sm text-slate-500">
        Đã có tài khoản? <Link to="/login" className="font-semibold text-brand-600">Đăng nhập</Link>
      </p>
    </AuthShell>
  )
}
