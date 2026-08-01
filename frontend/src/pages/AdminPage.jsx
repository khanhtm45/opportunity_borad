import { useEffect, useState, useCallback } from 'react'
import { adminApi } from '../api/admin.js'
import { useAuth } from '../context/AuthContext.jsx'
import { InlineLoader } from '../components/Splash.jsx'
import { OPP_STATUS_STYLES, STATUS_LABELS, APP_STATUS_LABELS, APP_STATUS_STYLES } from '../lib/constants.js'
import { asset } from '../lib/assets.js'

function StatCard({ label, value, hint, tone }) {
  const tones = {
    brand: 'bg-brand-500',
    accent: 'bg-accent-500',
    gold: 'bg-gold-500',
    emerald: 'bg-emerald-500',
  }
  return (
    <div className="rounded-2xl border border-slate-100 bg-white p-5 shadow-card">
      <div className="flex items-center justify-between">
        <div>
          <p className="text-xs font-medium uppercase tracking-wide text-slate-400">{label}</p>
          <p className="mt-1 text-2xl font-extrabold text-slate-800">{value}</p>
          {hint && <p className="mt-0.5 text-xs text-slate-400">{hint}</p>}
        </div>
        <div className={`flex h-10 w-10 items-center justify-center rounded-xl text-white ${tones[tone] || 'bg-brand-500'}`}>
          ◆
        </div>
      </div>
    </div>
  )
}

export default function AdminPage() {
  const { user } = useAuth()
  const [tab, setTab] = useState('dashboard')
  const [queue, setQueue] = useState([])
  const [users, setUsers] = useState([])
  const [analytics, setAnalytics] = useState(null)
  const [loading, setLoading] = useState(true)
  const [msg, setMsg] = useState('')

  const loadQueue = useCallback(() => adminApi.moderationQueue().then((r) => setQueue(r || [])), [])
  const loadUsers = useCallback(() => adminApi.users().then((r) => setUsers(r || [])), [])
  const loadAnalytics = useCallback(() => adminApi.analytics().then((r) => setAnalytics(r)).catch(() => null), [])

  const refresh = useCallback(() => {
    setLoading(true)
    Promise.all([loadQueue(), loadUsers(), loadAnalytics()]).finally(() => setLoading(false))
  }, [loadQueue, loadUsers, loadAnalytics])

  useEffect(() => { if (user?.role === 'ADMIN') refresh(); else setLoading(false) }, [user, refresh])

  const act = async (id, action, body) => {
    try {
      if (action === 'approve') await adminApi.approve(id)
      if (action === 'reject') { const r = prompt('Lý do từ chối:'); if (!r) return; await adminApi.reject(id, r) }
      if (action === 'feature') await adminApi.feature(id)
      setMsg(`✅ Đã ${action === 'approve' ? 'duyệt' : action === 'reject' ? 'từ chối' : 'đánh dấu Featured'} #${id.slice(0, 8)}`)
      refresh()
    } catch (e) { setMsg(e.response?.data?.error?.message || 'Lỗi') }
  }
  const verifyOrg = async (userId) => { await adminApi.verifyOrg(userId); setMsg('✅ Đã duyệt tổ chức'); refresh() }

  if (user?.role !== 'ADMIN')
    return <div className="py-16 text-center text-slate-400">Trang dành cho Quản trị viên.</div>

  const menu = [
    ['dashboard', '📊 Dashboard'],
    ['queue', '📝 Kiểm duyệt'],
    ['users', '👥 Người dùng'],
  ]

  const a = analytics || {}
  const pendingCount = queue.length

  return (
    <div className="flex gap-6">
      {/* Sidebar */}
      <aside className="hidden w-56 shrink-0 md:block">
        <div className="sticky top-20 space-y-1">
          <div className="mb-4 flex items-center justify-between rounded-2xl bg-slate-900 bg-cover bg-center px-4 py-3 text-white shadow-card" style={{ backgroundImage: `url(${asset('ob-network.svg')})` }}>
            <span className="text-lg font-extrabold drop-shadow">Admin Panel</span>
          </div>
          {menu.map(([k, v]) => (
            <button key={k} onClick={() => setTab(k)}
              className={`flex w-full items-center justify-between rounded-xl px-4 py-2.5 text-sm font-medium transition ${
                tab === k ? 'bg-brand-500 text-white shadow-card' : 'text-slate-600 hover:bg-slate-100'}`}>
              <span>{v}</span>
              {k === 'queue' && pendingCount > 0 && (
                <span className="rounded-full bg-accent-500 px-2 py-0.5 text-xs text-white">{pendingCount}</span>
              )}
            </button>
          ))}
        </div>
      </aside>

      {/* Main */}
      <div className="min-w-0 flex-1 space-y-4">
        <div className="flex items-center justify-between">
          <div>
            <h1 className="text-xl font-bold text-slate-800">🛡️ Quản trị hệ thống</h1>
            <p className="text-xs text-slate-400">Xin chào, {user?.fullName || user?.email}</p>
          </div>
          {msg && <div className="rounded-lg bg-emerald-50 px-3 py-2 text-sm font-medium text-emerald-700">{msg}</div>}
        </div>

        {/* Mobile tabs */}
        <div className="flex gap-2 md:hidden">
          {menu.map(([k, v]) => (
            <button key={k} onClick={() => setTab(k)}
              className={`flex-1 rounded-xl px-3 py-2 text-xs font-medium ${tab === k ? 'bg-brand-500 text-white' : 'bg-white text-slate-600 border border-slate-200'}`}>
              {v}
            </button>
          ))}
        </div>

        {loading ? <InlineLoader /> : (
          <>
            {tab === 'dashboard' && (
              <div className="space-y-4">
                <div className="grid grid-cols-2 gap-3 lg:grid-cols-4">
                  <StatCard label="Người dùng" value={a.users ?? 0} hint={`${a.students ?? 0} sinh viên`} tone="brand" />
                  <StatCard label="Cơ hội" value={a.opportunities ?? 0} hint={`${a.providers ?? 0} nhà tuyển dụng`} tone="accent" />
                  <StatCard label="Đơn ứng tuyển" value={a.applications ?? 0} hint="tổng đơn" tone="gold" />
                  <StatCard label="Chờ duyệt" value={pendingCount} hint="cần xử lý" tone="emerald" />
                </div>

                <div className="grid grid-cols-1 gap-4 lg:grid-cols-2">
                  {/* Recent pending */}
                  <div className="rounded-2xl border border-slate-100 bg-white p-4 shadow-card">
                    <div className="mb-3 flex items-center justify-between">
                      <h3 className="text-sm font-bold text-slate-700">Tin chờ duyệt gần đây</h3>
                      <button onClick={() => setTab('queue')} className="text-xs text-brand-600 hover:underline">Xem tất cả</button>
                    </div>
                    {queue.length === 0 ? <p className="py-8 text-center text-sm text-slate-400">Không có tin chờ duyệt.</p> :
                      <ul className="divide-y divide-slate-50">
                        {queue.slice(0, 5).map((o) => (
                          <li key={o.oppId} className="flex items-center justify-between py-2">
                            <div>
                              <p className="text-sm font-medium text-slate-700">{o.title}</p>
                              <p className="text-xs text-slate-400">{o.orgName}</p>
                            </div>
                            <span className={`chip ${OPP_STATUS_STYLES[o.status] || 'bg-slate-100'}`}>{STATUS_LABELS[o.status] || o.status}</span>
                          </li>
                        ))}
                      </ul>}
                  </div>

                  {/* Recent users */}
                  <div className="rounded-2xl border border-slate-100 bg-white p-4 shadow-card">
                    <div className="mb-3 flex items-center justify-between">
                      <h3 className="text-sm font-bold text-slate-700">Người dùng mới</h3>
                      <button onClick={() => setTab('users')} className="text-xs text-brand-600 hover:underline">Xem tất cả</button>
                    </div>
                    {users.length === 0 ? <p className="py-8 text-center text-sm text-slate-400">Chưa có user.</p> :
                      <ul className="divide-y divide-slate-50">
                        {users.slice(0, 5).map((u) => (
                          <li key={u.userId} className="flex items-center justify-between py-2">
                            <div>
                              <p className="text-sm font-medium text-slate-700">{u.fullName || u.email}</p>
                              <p className="text-xs text-slate-400">{u.role} · {u.status}</p>
                            </div>
                            {u.role === 'PROVIDER' && (
                              <span className={`chip ${u.status === 'ACTIVE' ? 'bg-emerald-50 text-emerald-700' : 'bg-amber-50 text-amber-700'}`}>
                                {u.status === 'ACTIVE' ? 'Đã duyệt' : 'Chờ duyệt'}
                              </span>
                            )}
                          </li>
                        ))}
                      </ul>}
                  </div>
                </div>
              </div>
            )}

            {tab === 'queue' && (
              <div className="space-y-3">
                <p className="text-xs text-slate-400">{queue.length} chờ duyệt</p>
                {queue.length === 0 ? <div className="py-16 text-center text-slate-400">Không có tin chờ duyệt.</div> :
                  queue.map((o) => (
                    <div key={o.oppId} className="rounded-2xl border border-slate-100 bg-white p-4 shadow-card">
                      <div className="flex items-start justify-between gap-4">
                        <div>
                          <h3 className="font-bold text-slate-800">{o.title}</h3>
                          <p className="text-xs text-slate-400">{o.orgName} · {o.categoryCode} · {o.location}</p>
                        </div>
                        <span className={`chip ${OPP_STATUS_STYLES[o.status] || 'bg-slate-100'}`}>{STATUS_LABELS[o.status] || o.status}</span>
                      </div>
                      <div className="mt-3 flex flex-wrap gap-2">
                        <button onClick={() => act(o.oppId, 'approve')} className="btn-primary px-4 py-2 text-xs">Duyệt</button>
                        <button onClick={() => act(o.oppId, 'reject')} className="btn-ghost px-4 py-2 text-xs text-rose-600">Từ chối</button>
                        <button onClick={() => act(o.oppId, 'feature')} className="btn-ghost px-4 py-2 text-xs text-gold-700">★ Featured</button>
                      </div>
                    </div>
                  ))}
              </div>
            )}

            {tab === 'users' && (
              <div className="overflow-hidden rounded-2xl border border-slate-100 bg-white shadow-card">
                <div className="flex items-center justify-between border-b border-slate-100 px-4 py-3">
                  <h3 className="text-sm font-bold text-slate-700">Danh sách người dùng</h3>
                  <span className="rounded-full bg-brand-500 px-2 py-0.5 text-xs text-white">{users.length}</span>
                </div>
                <table className="w-full text-sm">
                  <thead className="bg-slate-50 text-left text-xs uppercase text-slate-400">
                    <tr>
                      <th className="px-4 py-2">#</th>
                      <th className="px-4 py-2">Họ tên</th>
                      <th className="px-4 py-2">Email</th>
                      <th className="px-4 py-2">Vai trò</th>
                      <th className="px-4 py-2">Trạng thái</th>
                      <th className="px-4 py-2"></th>
                    </tr>
                  </thead>
                  <tbody className="divide-y divide-slate-50">
                    {users.map((u, i) => (
                      <tr key={u.userId} className="hover:bg-slate-50">
                        <td className="px-4 py-2 text-slate-400">{i + 1}</td>
                        <td className="px-4 py-2 font-medium text-slate-700">{u.fullName || '—'}</td>
                        <td className="px-4 py-2 text-slate-500">{u.email}</td>
                        <td className="px-4 py-2">
                          <span className={`chip ${u.role === 'ADMIN' ? 'bg-gold-50 text-gold-700' : u.role === 'PROVIDER' ? 'bg-accent-50 text-accent-700' : 'bg-brand-50 text-brand-700'}`}>{u.role}</span>
                        </td>
                        <td className="px-4 py-2">
                          <span className={`chip ${u.status === 'ACTIVE' ? 'bg-emerald-50 text-emerald-700' : 'bg-amber-50 text-amber-700'}`}>{u.status}</span>
                        </td>
                        <td className="px-4 py-2 text-right">
                          {u.role === 'PROVIDER' && u.status !== 'ACTIVE' && (
                            <button onClick={() => verifyOrg(u.userId)} className="chip-btn text-brand-700">Duyệt org</button>
                          )}
                        </td>
                      </tr>
                    ))}
                    {users.length === 0 && (
                      <tr><td colSpan={6} className="px-4 py-8 text-center text-slate-400">Chưa có user.</td></tr>
                    )}
                  </tbody>
                </table>
              </div>
            )}
          </>
        )}
      </div>
    </div>
  )
}
