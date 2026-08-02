import { useState } from 'react'
import { useNavigate, Link } from 'react-router-dom'
import { useAuth } from '../context/AuthContext.jsx'
import { InlineLoader } from '../components/Splash.jsx'
import { asset } from '../lib/assets.js'

export default function LoginPage() {
  const { login } = useAuth()
  const navigate = useNavigate()
  const [email, setEmail] = useState('')
  const [password, setPassword] = useState('')
  const [err, setErr] = useState('')
  const [busy, setBusy] = useState(false)

  const submit = async (e) => {
    e.preventDefault()
    setBusy(true); setErr('')
    try {
      const data = await login(email, password)
      const role = data.role || (JSON.parse(atob(data.accessToken.split('.')[1])).role)
      navigate(role === 'PROVIDER' ? '/provider' : role === 'ADMIN' ? '/admin' : '/')
    } catch (e2) {
      setErr(e2.response?.data?.error?.message || 'Đăng nhập thất bại')
    } finally { setBusy(false) }
  }

  return (
    <AuthShell title="Đăng nhập">
      <form onSubmit={submit} className="space-y-4">
        {err && <div className="rounded-lg bg-rose-50 px-3 py-2 text-sm text-rose-600">{err}</div>}
        <input className="input-base" type="email" required placeholder="Email" value={email} onChange={(e) => setEmail(e.target.value)} />
        <input className="input-base" type="password" required placeholder="Mật khẩu" value={password} onChange={(e) => setPassword(e.target.value)} />
        <button className="btn-primary w-full" disabled={busy}>{busy ? <InlineLoader /> : 'Đăng nhập'}</button>
      </form>
      <p className="mt-4 text-center text-sm text-slate-500">
        Chưa có tài khoản? <Link to="/register" className="font-semibold text-brand-600">Đăng ký</Link>
      </p>
    </AuthShell>
  )
}

export function AuthShell({ title, children, wide = false }) {
  return (
    <div
      className="flex min-h-[78vh] items-center justify-center px-4 py-10 bg-slate-900 bg-cover bg-center"
      style={{ backgroundImage: `url(${asset('ob-network.svg')})` }}
    >
      <div className={`w-full rounded-3xl border border-white/10 bg-white/95 p-8 shadow-2xl backdrop-blur-sm ${wide ? 'max-w-2xl' : 'max-w-md'}`}>
        <div className="mb-6 flex flex-col items-center">
          <img src={asset('logo.png')} alt="logo" className="h-14 w-14 drop-shadow" />
          <h1 className="mt-3 text-xl font-extrabold text-brand-700">{title}</h1>
        </div>
        {children}
      </div>
    </div>
  )
}
