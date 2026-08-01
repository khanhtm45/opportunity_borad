import { useState, useEffect } from 'react'
import { Link } from 'react-router-dom'
import { useAuth } from '../context/AuthContext.jsx'
import { studentApi } from '../api/student.js'
import { InlineLoader } from '../components/Splash.jsx'
import { APP_STATUS_LABELS, APP_STATUS_STYLES, fmtDate } from '../lib/constants.js'
import { asset } from '../lib/assets.js'

export default function DashboardPage() {
  const { user } = useAuth()
  const [tab, setTab] = useState('applications')
  const [apps, setApps] = useState([])
  const [marks, setMarks] = useState([])
  const [notifs, setNotifs] = useState([])
  const [loading, setLoading] = useState(false)

  const loadTab = (t) => {
    setTab(t); setLoading(true)
    if (t === 'applications') studentApi.myApplications().then((r) => setApps(r)).finally(() => setLoading(false))
    else if (t === 'bookmarks') {
      import('../api/client.js').then(({ default: api }) =>
        api.get('/me/bookmarks?size=50').then((r) => setMarks((r.data.items || []).map((x) => x.opportunity))).finally(() => setLoading(false)))
    }
    else if (t === 'notifications') studentApi.notifications().then((r) => setNotifs(r)).finally(() => setLoading(false))
  }
  useEffect(() => { loadTab('applications') }, []) // eslint-disable-line

  if (!user) return <div className="py-16 text-center text-slate-400">Vui lòng đăng nhập.</div>

  const menu = [
    ['applications', '📨 Đơn ứng tuyển'],
    ['bookmarks', '🔖 Đã lưu'],
    ['notifications', '🔔 Thông báo'],
    ['profile', '👤 Hồ sơ'],
  ]

  return (
    <div className="flex gap-6">
      {/* Sidebar nav trái (mẫu 3) */}
      <aside className="hidden w-56 shrink-0 md:block">
        <div className="sticky top-20 space-y-1">
          <div className="mb-3 flex items-center gap-2 rounded-2xl bg-slate-900 px-4 py-3 text-white shadow-card">
            <span className="text-lg font-extrabold">{user.fullName || user.email}</span>
          </div>
          {menu.map(([k, v]) => (
            <button key={k} onClick={() => loadTab(k)}
              className={`flex w-full items-center rounded-xl px-4 py-2.5 text-sm font-medium transition ${
                tab === k ? 'bg-brand-500 text-white shadow-card' : 'text-slate-600 hover:bg-slate-100'}`}>
              {v}
            </button>
          ))}
          <Link to="/provider" className="mt-2 block rounded-xl border border-slate-200 px-4 py-2.5 text-center text-sm text-slate-500 hover:bg-slate-50">🏢 Nhà tuyển dụng</Link>
          <Link to="/admin" className="block rounded-xl border border-slate-200 px-4 py-2.5 text-center text-sm text-slate-500 hover:bg-slate-50">🛡️ Quản trị</Link>
        </div>
      </aside>

      {/* Body lớn */}
      <div className="min-w-0 flex-1 space-y-4">
        {/* Banner phải */}
        <div className="overflow-hidden rounded-3xl bg-slate-900 bg-cover bg-center p-6 text-white shadow-card" style={{ backgroundImage: `url(${asset('ob-network.svg')})` }}>
          <h1 className="text-xl font-extrabold drop-shadow md:text-2xl">Quản lý cá nhân</h1>
          <p className="mt-1 text-sm text-white/85">Theo dõi đơn ứng tuyển, cơ hội đã lưu và thông báo.</p>
        </div>

        {loading ? <InlineLoader /> : (
          <>
            {tab === 'applications' && (
              <div className="space-y-2">
                {apps.length === 0 ? <p className="py-12 text-center text-slate-400">Chưa có đơn nào.</p> :
                  apps.map((a) => (
                    <Link key={a.appId} to={`/opportunities/${a.slug}`}
                      className="flex items-center justify-between rounded-xl border border-slate-100 bg-white p-3 shadow-card">
                      <div>
                        <p className="text-sm font-semibold text-slate-700">{a.title}</p>
                        <p className="text-xs text-slate-400">Nộp {fmtDate(a.appliedAt)}</p>
                      </div>
                      <span className={`chip ${APP_STATUS_STYLES[a.status] || 'bg-slate-100'}`}>{APP_STATUS_LABELS[a.status] || a.status}</span>
                    </Link>
                  ))}
              </div>
            )}
            {tab === 'bookmarks' && (
              <div className="grid grid-cols-1 gap-4 sm:grid-cols-2 lg:grid-cols-3">
                {marks.length === 0 ? <p className="py-12 text-center text-slate-400">Chưa lưu cơ hội nào.</p> :
                  marks.map((o) => (
                    <Link key={o.oppId} to={`/opportunities/${o.slug}`}
                      className="rounded-2xl border border-slate-100 bg-white p-4 shadow-card">
                      <h3 className="font-bold text-slate-800">{o.title}</h3>
                      <p className="text-xs text-slate-400">{o.orgName}</p>
                    </Link>
                  ))}
              </div>
            )}
            {tab === 'notifications' && (
              <div className="space-y-2">
                {notifs.length === 0 ? <p className="py-12 text-center text-slate-400">Chưa có thông báo.</p> :
                  notifs.map((n) => (
                    <div key={n.notificationId} className={`rounded-xl border p-3 shadow-card ${n.read ? 'border-slate-100 bg-white' : 'border-brand-200 bg-brand-50'}`}>
                      <p className="text-sm font-medium text-slate-700">{n.title}</p>
                      {n.body && <p className="text-xs text-slate-500">{n.body}</p>}
                    </div>
                  ))}
              </div>
            )}
            {tab === 'profile' && (
              <div className="rounded-2xl border border-slate-100 bg-white p-6 shadow-card">
                <h3 className="font-bold text-slate-800">{user.fullName || '—'}</h3>
                <p className="text-sm text-slate-500">{user.email}</p>
                <p className="mt-2 text-xs text-slate-400">Vai trò: {user.role} · Trạng thái: {user.status}</p>
              </div>
            )}
          </>
        )}
      </div>
    </div>
  )
}
