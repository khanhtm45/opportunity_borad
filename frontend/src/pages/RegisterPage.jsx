import { useState } from 'react'
import { Link } from 'react-router-dom'
import { useAuth } from '../context/AuthContext.jsx'
import { InlineLoader } from '../components/Splash.jsx'
import { AuthShell } from './LoginPage.jsx'
import { COMPANY_SIZE_LABELS } from '../lib/constants.js'
import FileUploadButton from '../components/FileUploadButton.jsx'

const ROLES = [
  { v: 'STUDENT', l: 'Sinh viên', d: 'Tìm cơ hội, ứng tuyển, lưu tin' },
  { v: 'PROVIDER', l: 'Nhà tuyển dụng', d: 'Đăng tin (cần Admin duyệt)' },
]

const ORG_DOC_LABELS = {
  BUSINESS_LICENSE: 'Giấy phép kinh doanh',
  TAX_CODE: 'Mã số thuế',
  IDENTITY: 'Giấy tờ định danh',
  OTHER: 'Khác',
}

const EMPTY_ORG_DOC = { docType: 'BUSINESS_LICENSE', title: '', fileUrl: '' }

export default function RegisterPage() {
  const { register } = useAuth()
  const [form, setForm] = useState({
    role: 'STUDENT', email: '', password: '', fullName: '', orgName: '',
    contactPhone: '', taxCode: '', address: '', industry: '', companySize: 'UNKNOWN',
    documents: [{ ...EMPTY_ORG_DOC }],
  })
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

  const submit = async (e) => {
    e.preventDefault()
    setBusy(true); setErr('')
    try {
      const payload = { ...form }
      if (form.role === 'PROVIDER') {
        const documents = (form.documents || [])
          .map((d) => ({
            docType: d.docType || 'BUSINESS_LICENSE',
            title: (d.title || '').trim(),
            fileUrl: (d.fileUrl || '').trim(),
          }))
          .filter((d) => d.title && d.fileUrl)
        if (documents.length < 1) {
          setErr('Nhà tuyển dụng cần ít nhất 1 hồ sơ tổ chức (upload file hoặc URL)')
          setBusy(false)
          return
        }
        payload.documents = documents
        payload.orgName = form.orgName || form.fullName
      }
      await register(payload)
      setDone(true)
    } catch (e2) {
      setErr(e2.response?.data?.error?.message || 'Đăng ký thất bại')
    } finally { setBusy(false) }
  }

  if (done) return (
    <AuthShell title="Đăng ký thành công">
      <p className="text-center text-sm text-slate-600">
        Chúng tôi đã ghi nhận tài khoản. Vui lòng xác thực email để đăng nhập.
      </p>
      <Link to="/login" className="btn-primary mt-4 w-full">Đến trang đăng nhập</Link>
    </AuthShell>
  )

  return (
    <AuthShell title="Tạo tài khoản">
      <form onSubmit={submit} className="space-y-4">
        {err && <div className="rounded-lg bg-rose-50 px-3 py-2 text-sm text-rose-600">{err}</div>}
        <div className="grid grid-cols-2 gap-3">
          {ROLES.map((r) => (
            <button type="button" key={r.v} onClick={() => setForm((f) => ({ ...f, role: r.v }))}
              className={`rounded-xl border p-3 text-left text-xs transition ${
                form.role === r.v ? 'border-brand-500 bg-brand-50' : 'border-slate-200 hover:border-brand-300'}`}>
              <div className="font-bold text-slate-700">{r.l}</div>
              <div className="text-slate-400">{r.d}</div>
            </button>
          ))}
        </div>
        <input className="input-base" placeholder="Họ tên" value={form.fullName} onChange={set('fullName')} required />
        {form.role === 'PROVIDER' && (
          <>
            <input className="input-base" placeholder="Tên tổ chức" value={form.orgName} onChange={set('orgName')} required />
            <input className="input-base" placeholder="SĐT liên hệ" value={form.contactPhone} onChange={set('contactPhone')} />
            <input className="input-base" placeholder="Mã số thuế (MST)" value={form.taxCode} onChange={set('taxCode')} />
            <input className="input-base" placeholder="Địa chỉ công ty" value={form.address} onChange={set('address')} />
            <input className="input-base" placeholder="Lĩnh vực hoạt động" value={form.industry} onChange={set('industry')} />
            <select className="input-base" value={form.companySize} onChange={set('companySize')}>
              {Object.entries(COMPANY_SIZE_LABELS).map(([k, v]) => <option key={k} value={k}>{v}</option>)}
            </select>
          </>
        )}
        <input className="input-base" type="email" placeholder="Email" value={form.email} onChange={set('email')} required />
        <input className="input-base" type="password" placeholder="Mật khẩu (≥8 ký tự)" value={form.password} onChange={set('password')} required minLength={8} />
        {form.role === 'PROVIDER' && (
          <div className="space-y-2 rounded-xl border border-slate-100 bg-slate-50/80 p-3">
            <p className="text-sm font-semibold text-slate-700">Hồ sơ tổ chức <span className="font-normal text-slate-400">(bắt buộc ≥1)</span></p>
            <p className="text-[11px] text-slate-400">File upload S3 private + mã hóa AES — không lộ public.</p>
            {(form.documents || []).map((d, idx) => (
              <div key={idx} className="grid gap-2">
                <select className="input-base" value={d.docType} onChange={(e) => setDoc(idx, { docType: e.target.value })}>
                  {Object.entries(ORG_DOC_LABELS).map(([k, v]) => <option key={k} value={k}>{v}</option>)}
                </select>
                <input className="input-base" placeholder="Tiêu đề hồ sơ" value={d.title} onChange={(e) => setDoc(idx, { title: e.target.value })} required />
                <div className="flex flex-wrap gap-2">
                  <input className="input-base min-w-0 flex-1" placeholder="ob-s3://… sau khi upload" value={d.fileUrl} onChange={(e) => setDoc(idx, { fileUrl: e.target.value })} required />
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
              </div>
            ))}
          </div>
        )}
        <button className="btn-primary w-full" disabled={busy}>{busy ? <InlineLoader /> : 'Đăng ký'}</button>
      </form>
      <p className="mt-4 text-center text-sm text-slate-500">
        Đã có tài khoản? <Link to="/login" className="font-semibold text-brand-600">Đăng nhập</Link>
      </p>
    </AuthShell>
  )
}
