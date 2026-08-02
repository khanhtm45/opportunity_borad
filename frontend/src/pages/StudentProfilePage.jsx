import { useEffect, useState } from 'react'
import { Link, useNavigate } from 'react-router-dom'
import { useAuth } from '../context/AuthContext.jsx'
import { studentApi } from '../api/student.js'
import { uploadFile, mediaSrc } from '../api/upload.js'
import { InlineLoader } from '../components/Splash.jsx'
import { asset } from '../lib/assets.js'

export default function StudentProfilePage() {
  const { user } = useAuth()
  const navigate = useNavigate()
  const [loading, setLoading] = useState(true)
  const [saving, setSaving] = useState(false)
  const [uploading, setUploading] = useState(false)
  const [msg, setMsg] = useState('')
  const [form, setForm] = useState({
    major: '',
    university: '',
    universityYear: '',
    cvUrl: '',
    skillsText: '',
    bio: '',
  })
  const [hasCv, setHasCv] = useState(false)
  const [cvView, setCvView] = useState('')

  const onPickCv = async (e) => {
    const file = e.target.files?.[0]
    e.target.value = ''
    if (!file) return
    setMsg('')
    setUploading(true)
    try {
      const up = await uploadFile(file, 'cv')
      setForm((f) => ({ ...f, cvUrl: up.url }))
      setHasCv(true)
      const p = await studentApi.updateProfile({ cvUrl: up.url })
      setHasCv(!!p.hasCv)
      setCvView(mediaSrc(up.viewUrl))
      setMsg('✅ Đã upload CV (S3 private + AES256) và lưu hồ sơ')
    } catch (err) {
      setMsg(err.response?.data?.error?.message || 'Upload S3 thất bại — kiểm tra AWS keys')
    } finally {
      setUploading(false)
    }
  }

  useEffect(() => {
    if (!user) return
    if (user.role !== 'STUDENT') {
      setLoading(false)
      return
    }
    setLoading(true)
    studentApi.profile()
      .then((p) => {
        setForm({
          major: p.major || '',
          university: p.university || '',
          universityYear: p.universityYear != null ? String(p.universityYear) : '',
          cvUrl: p.cvUrl || '',
          skillsText: (p.skills || []).join(', '),
          bio: p.bio || '',
        })
        setHasCv(!!p.hasCv)
      })
      .catch((e) => setMsg(e.response?.data?.error?.message || 'Không tải được hồ sơ'))
      .finally(() => setLoading(false))
  }, [user])

  const save = async (e) => {
    e.preventDefault()
    setMsg('')
    setSaving(true)
    try {
      const skills = form.skillsText
        .split(/[,;\n]/)
        .map((s) => s.trim())
        .filter(Boolean)
      const body = {
        major: form.major.trim(),
        university: form.university.trim(),
        universityYear: form.universityYear ? Number(form.universityYear) : null,
        cvUrl: form.cvUrl.trim(),
        skills,
        bio: form.bio.trim(),
      }
      const p = await studentApi.updateProfile(body)
      setHasCv(!!p.hasCv)
      setForm((f) => ({ ...f, cvUrl: p.cvUrl || '' }))
      setMsg('✅ Đã lưu hồ sơ & CV')
    } catch (err) {
      setMsg(err.response?.data?.error?.message || 'Lỗi khi lưu')
    } finally {
      setSaving(false)
    }
  }

  if (!user) {
    return (
      <div className="py-16 text-center text-slate-400">
        Vui lòng <Link to="/login" className="text-brand-600 hover:underline">đăng nhập</Link> để quản lý CV.
      </div>
    )
  }
  if (user.role !== 'STUDENT') {
    return <div className="py-16 text-center text-slate-400">Trang hồ sơ & CV dành cho sinh viên.</div>
  }
  if (loading) return <div className="py-16 text-center"><InlineLoader label="Đang tải hồ sơ…" /></div>

  return (
    <div className="mx-auto max-w-2xl space-y-5">
      <div
        className="overflow-hidden rounded-3xl bg-slate-900 bg-cover bg-center p-6 text-white shadow-card"
        style={{ backgroundImage: `url(${asset('ob-network.svg')})` }}
      >
        <button type="button" onClick={() => navigate('/me')} className="text-sm text-white/80 hover:text-white">← Cá nhân</button>
        <h1 className="mt-2 text-xl font-extrabold drop-shadow md:text-2xl">Hồ sơ & CV</h1>
        <p className="mt-1 text-sm text-white/85">Tải CV lên một lần — dùng lại khi nộp đơn nội bộ.</p>
      </div>

      <form onSubmit={save} className="space-y-4 rounded-2xl border border-slate-100 bg-white p-5 shadow-card">
        <div>
          <p className="text-sm font-semibold text-slate-800">{user.fullName || '—'}</p>
          <p className="text-xs text-slate-400">{user.email}</p>
        </div>

        <div className="rounded-xl border border-brand-100 bg-brand-50/60 p-4">
          <div className="mb-2 flex items-center justify-between gap-2">
            <h2 className="text-sm font-bold text-slate-800">CV của bạn</h2>
            <span className={`chip ${hasCv ? 'bg-emerald-50 text-emerald-700' : 'bg-amber-50 text-amber-700'}`}>
              {hasCv ? 'Đã có CV' : 'Chưa có CV'}
            </span>
          </div>
          <p className="mb-3 text-xs text-slate-500">
            Upload PDF/DOC/DOCX (≤10MB) — S3 private + mã hóa AES256, không public bucket.
          </p>
          <label className="btn-primary inline-flex cursor-pointer">
            <input type="file" accept=".pdf,.doc,.docx,application/pdf" className="hidden" onChange={onPickCv} disabled={uploading} />
            {uploading ? 'Đang upload S3…' : 'Chọn file CV & upload'}
          </label>
          <input
            className="input-base mt-3"
            placeholder="ob-s3://… (tự điền sau upload)"
            value={form.cvUrl}
            onChange={(e) => setForm({ ...form, cvUrl: e.target.value })}
          />
          {(cvView || form.cvUrl) && !form.cvUrl.startsWith('ob-s3://') && (
            <a href={mediaSrc(cvView || form.cvUrl)} target="_blank" rel="noreferrer" className="mt-2 inline-block text-xs text-brand-600 hover:underline">
              Xem CV →
            </a>
          )}
          {form.cvUrl.startsWith('ob-s3://') && cvView && (
            <a href={cvView} target="_blank" rel="noreferrer" className="mt-2 inline-block text-xs text-brand-600 hover:underline">
              Xem CV (link ký tạm) →
            </a>
          )}
        </div>

        <div className="grid gap-3 sm:grid-cols-2">
          <label className="block text-xs text-slate-500">
            Trường / Đại học
            <input className="input-base mt-1" value={form.university}
              onChange={(e) => setForm({ ...form, university: e.target.value })}
              placeholder="VD: UEH" />
          </label>
          <label className="block text-xs text-slate-500">
            Ngành
            <input className="input-base mt-1" value={form.major}
              onChange={(e) => setForm({ ...form, major: e.target.value })}
              placeholder="VD: Công nghệ thông tin" />
          </label>
          <label className="block text-xs text-slate-500">
            Năm học (1–8)
            <input className="input-base mt-1" type="number" min={1} max={8} value={form.universityYear}
              onChange={(e) => setForm({ ...form, universityYear: e.target.value })}
              placeholder="3" />
          </label>
          <label className="block text-xs text-slate-500">
            Kỹ năng (cách nhau bởi dấu phẩy)
            <input className="input-base mt-1" value={form.skillsText}
              onChange={(e) => setForm({ ...form, skillsText: e.target.value })}
              placeholder="Java, React, Figma" />
          </label>
        </div>

        <label className="block text-xs text-slate-500">
          Giới thiệu ngắn
          <textarea className="input-base mt-1 min-h-[6rem]" value={form.bio}
            onChange={(e) => setForm({ ...form, bio: e.target.value })}
            placeholder="Mục tiêu nghề nghiệp, điểm mạnh…" />
        </label>

        {msg && <p className="text-sm text-slate-600">{msg}</p>}

        <div className="flex flex-wrap gap-2">
          <button type="submit" className="btn-primary" disabled={saving}>
            {saving ? 'Đang lưu…' : 'Lưu hồ sơ & CV'}
          </button>
          <Link to="/board" className="btn-ghost">Đến bảng tin</Link>
        </div>
      </form>
    </div>
  )
}
