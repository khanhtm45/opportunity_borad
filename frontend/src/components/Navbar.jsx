import { Link, useNavigate } from 'react-router-dom'
import { useAuth } from '../context/AuthContext.jsx'

export default function Navbar() {
  const { user, logout } = useAuth()
  const navigate = useNavigate()

  return (
    <header className="sticky top-0 z-40 border-b border-slate-200 bg-white/80 backdrop-blur">
      <div className="mx-auto flex h-16 max-w-6xl items-center justify-between px-4">
        <Link to="/" className="flex items-center gap-2">
          <img src="/logo.png" alt="logo" className="h-9 w-9" />
          <span className="text-lg font-extrabold tracking-tight text-brand-700">
            Opportunity<span className="text-accent-500">Board</span>
          </span>
        </Link>

        <nav className="hidden items-center gap-5 text-sm font-medium text-slate-600 md:flex">
          <Link to="/home" className="hover:text-brand-600">Trang chủ</Link>
          <Link to="/board" className="hover:text-brand-600">Bảng tin</Link>
          <Link to="/explore" className="hover:text-brand-600">Khám phá</Link>
          {user?.role === 'PROVIDER' && (
            <Link to="/provider" className="hover:text-brand-600">Nhà tuyển dụng</Link>
          )}
          {user?.role === 'ADMIN' && (
            <Link to="/admin" className="hover:text-brand-600">Quản trị</Link>
          )}
          {user?.role === 'STUDENT' && (
            <Link to="/me" className="hover:text-brand-600">Cá nhân</Link>
          )}
        </nav>

        <div className="flex items-center gap-3">
          {user ? (
            <>
              <span className="hidden text-sm text-slate-500 sm:inline">
                {user.fullName || user.email}
              </span>
              <button
                onClick={() => { logout(); navigate('/') }}
                className="btn-ghost px-3 py-2 text-xs"
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
