import { useState } from 'react'
import { useNavigate, Link } from 'react-router-dom'
import { useAuth } from '../context/AuthContext.jsx'
import { InlineLoader } from '../components/Splash.jsx'
import { AuthShell } from './LoginPage.jsx'

const ROLES = [
  { v: 'STUDENT', l: 'Sinh viên', d: 'Tìm cơ hội, ứng tuyển, lưu tin' },
  { v: 'PROVIDER', l: 'Nhà tuyển dụng', d: 'Đăng tin (cần Admin duyệt)' },
]

export default function RegisterPage() {
  const { register } = useAuth()
  const navigate = useNavigate()
  const [form, setForm] = useState({ role: 'STUDENT', email: '', password: '', fullName: '' })
  const [err, setErr] = useState('')
  const [busy, setBusy] = useState(false)
  const [done, setDone] = useState(false)

  const set = (k) => (e) => setForm((f) => ({ ...f, [k]: e.target.value }))

  const submit = async (e) => {
    e.preventDefault()
    setBusy(true); setErr('')
    try {
      await register(form)
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
        <input className="input-base" type="email" placeholder="Email" value={form.email} onChange={set('email')} required />
        <input className="input-base" type="password" placeholder="Mật khẩu (≥8 ký tự)" value={form.password} onChange={set('password')} required minLength={8} />
        <button className="btn-primary w-full" disabled={busy}>{busy ? <InlineLoader /> : 'Đăng ký'}</button>
      </form>
      <p className="mt-4 text-center text-sm text-slate-500">
        Đã có tài khoản? <Link to="/login" className="font-semibold text-brand-600">Đăng nhập</Link>
      </p>
    </AuthShell>
  )
}
