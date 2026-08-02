import { Link, useNavigate, useLocation } from 'react-router-dom'
import { useAuth } from '../context/AuthContext.jsx'
import { asset } from '../lib/assets.js'

export default function Navbar() {
  const { user, logout } = useAuth()
  const navigate = useNavigate()
  const { pathname } = useLocation()

  // Các mục nav chính (khớp thiết kế: Trang chủ / Bảng tin / Khám phá / Cá nhân)
  const navItems = [
    { to: '/home', label: 'Giới thiệu' },
    { to: '/board', label: 'Bảng tin' },
    { to: '/explore', label: 'Khám phá' },
  ]
  if (user) navItems.push({ to: '/me', label: 'Cá nhân' })

  const isActive = (to) => {
    if (to === '/me') return pathname === '/me' || pathname.startsWith('/me/')
    if (to === '/home') return pathname === '/' || pathname === '/home'
    return pathname === to || (to !== '/' && pathname.startsWith(to))
  }

  return (
    <header className="sticky top-0 z-40 border-b border-slate-200 bg-white/80 backdrop-blur">
      <div className="mx-auto flex h-16 max-w-6xl items-center justify-between px-4">
        <Link to="/" className="flex items-center gap-2">
          <img src={asset('logo.png')} alt="logo" className="h-9 w-9" />
          <span className="text-lg font-extrabold tracking-tight text-brand-700">
            Opportunity<span className="text-accent-500">Board</span>
          </span>
        </Link>

        <nav className="hidden items-center gap-6 text-sm font-medium text-slate-600 md:flex">
          {navItems.map((it) => (
            <Link
              key={it.to}
              to={it.to}
              className={`relative pb-1 transition-colors hover:text-brand-600 ${
                isActive(it.to) ? 'text-brand-600 after:absolute after:inset-x-0 after:-bottom-0.5 after:h-0.5 after:rounded-full after:bg-brand-500' : ''
              }`}
            >
              {it.label}
            </Link>
          ))}
        </nav>

        <div className="flex items-center gap-3">
          {user ? (
            <>
              <span className="hidden text-sm text-slate-500 sm:inline">
                {user.fullName || user.email}
              </span>
              <button
                onClick={() => { logout(); navigate('/') }}
                className="rounded-xl border border-brand-300 px-3 py-2 text-xs font-semibold text-brand-600 transition hover:bg-brand-50"
              >
                Đăng xuất
              </button>
            </>
          ) : (
            <>
              <Link to="/login" className="btn-ghost px-3 py-2 text-xs">Đăng nhập</Link>
              <Link to="/register" className="btn-primary px-3 py-2 text-xs">Đăng ký</Link>
            </>
          )}
        </div>
      </div>
    </header>
  )
}
