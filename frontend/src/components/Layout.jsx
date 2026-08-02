import Navbar from './Navbar.jsx'
import { Link, useLocation } from 'react-router-dom'
import { CATEGORY_LABELS } from '../lib/constants.js'

export default function Layout({ children }) {
  const { pathname } = useLocation()
  const isPitch = pathname === '/' || pathname === '/home'

  return (
    <div className="flex min-h-full flex-col bg-slate-50">
      <Navbar />
      <main className={isPitch ? 'w-full flex-1' : 'mx-auto w-full max-w-6xl flex-1 px-4 py-6'}>
        {children}
      </main>
      <footer className="mt-10 border-t border-slate-200 bg-white">
        <div className="mx-auto max-w-6xl px-4 py-8">
          <div className="grid gap-6 md:grid-cols-2">
            <div>
              <p className="text-sm font-bold text-brand-700">Opportunity Board</p>
              <p className="mt-1 text-xs text-slate-500">Bảng tin Cơ hội Sinh viên · Powered by OppHub</p>
            </div>
            <div>
              <p className="mb-2 text-xs font-bold uppercase tracking-wide text-slate-400">Danh mục cơ hội</p>
              <div className="flex flex-wrap gap-2">
                {Object.entries(CATEGORY_LABELS).map(([k, v]) => (
                  <Link key={k} to={`/explore?cat=${k}`} className="text-xs text-slate-500 hover:text-brand-600">
                    {v}
                  </Link>
                ))}
              </div>
            </div>
          </div>
          <p className="mt-6 border-t border-slate-100 pt-4 text-center text-xs text-slate-400">
            © 2026 Opportunity Board · Bảng tin Cơ hội Sinh viên
          </p>
        </div>
      </footer>
    </div>
  )
}
