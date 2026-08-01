import { useEffect, useState } from 'react'
import { Link } from 'react-router-dom'
import { studentApi } from '../api/student.js'
import { useAuth } from '../context/AuthContext.jsx'
import { InlineLoader } from '../components/Splash.jsx'
import { APP_STATUS_LABELS, APP_STATUS_STYLES, fmtDate } from '../lib/constants.js'

export default function MyApplicationsPage() {
  const { user } = useAuth()
  const [list, setList] = useState([])
  const [loading, setLoading] = useState(true)

  const load = () => studentApi.myApplications().then((r) => setList(r || [])).finally(() => setLoading(false))
  useEffect(() => { if (user) load() }, [user])

  if (!user) return null

  const withdraw = async (appId) => {
    if (!confirm('Rút đơn ứng tuyển?')) return
    await studentApi.withdraw(appId); load()
  }

  return (
    <div className="space-y-4">
      <h1 className="text-xl font-bold text-slate-800">Đơn ứng tuyển của tôi</h1>
      {loading ? <InlineLoader /> : list.length === 0 ? (
        <div className="py-16 text-center text-slate-400">Bạn chưa nộp đơn nào. <Link to="/" className="text-brand-600">Tìm cơ hội →</Link></div>
      ) : (
        <div className="space-y-2">
          {list.map((a) => (
            <div key={a.appId} className="flex items-center justify-between rounded-xl border border-slate-100 bg-white p-3 shadow-card">
              <div>
                <Link to={`/opportunities/${a.slug}`} className="text-sm font-semibold text-slate-700 hover:text-brand-700">{a.title}</Link>
                <p className="text-xs text-slate-400">Nộp {fmtDate(a.appliedAt)}</p>
              </div>
              <div className="flex items-center gap-2">
                <span className={`chip ${APP_STATUS_STYLES[a.status] || 'bg-slate-100'}`}>{APP_STATUS_LABELS[a.status] || a.status}</span>
                {(a.status === 'SUBMITTED' || a.status === 'REVIEWING') && (
                  <button onClick={() => withdraw(a.appId)} className="chip-btn text-rose-600">Rút</button>
                )}
              </div>
            </div>
          ))}
        </div>
      )}
    </div>
  )
}
