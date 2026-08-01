import { useEffect, useState } from 'react'
import { studentApi } from '../api/student.js'
import { useAuth } from '../context/AuthContext.jsx'
import { InlineLoader } from '../components/Splash.jsx'
import { fmtDateTime } from '../lib/constants.js'

export default function NotificationsPage() {
  const { user } = useAuth()
  const [list, setList] = useState([])
  const [loading, setLoading] = useState(true)

  const load = () => studentApi.notifications().then((r) => setList(r || [])).finally(() => setLoading(false))
  useEffect(() => { if (user) load() }, [user])

  if (!user) return null

  const markRead = async (id) => { await studentApi.markRead(id); load() }

  return (
    <div className="space-y-4">
      <h1 className="text-xl font-bold text-slate-800">Thông báo</h1>
      {loading ? <InlineLoader /> : list.length === 0 ? (
        <div className="py-16 text-center text-slate-400">Chưa có thông báo.</div>
      ) : (
        <div className="space-y-2">
          {list.map((n) => (
            <div key={n.notificationId} className={`rounded-xl border p-3 shadow-card ${n.read ? 'border-slate-100 bg-white' : 'border-brand-200 bg-brand-50'}`}>
              <div className="flex items-start justify-between gap-3">
                <div className="flex-1">
                  <p className="text-sm font-medium text-slate-700">{n.title}</p>
                  {n.body && <p className="text-xs text-slate-500">{n.body}</p>}
                  <p className="mt-1 text-xs text-slate-400">{fmtDateTime(n.createdAt)}</p>
                </div>
                {!n.read && <button onClick={() => markRead(n.notificationId)} className="chip-btn text-brand-700">Đã đọc</button>}
              </div>
            </div>
          ))}
        </div>
      )}
    </div>
  )
}
