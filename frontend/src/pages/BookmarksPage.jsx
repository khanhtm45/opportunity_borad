import { useEffect, useState } from 'react'
import api from '../api/client.js'
import OpportunityCard from '../components/OpportunityCard.jsx'
import { InlineLoader } from '../components/Splash.jsx'

export default function BookmarksPage() {
  const [items, setItems] = useState([])
  const [loading, setLoading] = useState(true)

  useEffect(() => {
    api.get('/me/bookmarks?size=50')
      // API trả OpportunityResponse trực tiếp trong items (không bọc { opportunity })
      .then((r) => setItems((r.data.items || []).map((x) => x?.opportunity || x).filter(Boolean)))
      .catch(() => setItems([]))
      .finally(() => setLoading(false))
  }, [])

  return (
    <div className="space-y-4">
      <h1 className="text-xl font-bold text-slate-800">🔖 Cơ hội đã lưu</h1>
      {loading ? <div className="py-16 text-center"><InlineLoader label="Đang tải…" /></div>
        : items.length === 0 ? <div className="py-16 text-center text-slate-400">Bạn chưa lưu cơ hội nào.</div>
        : <div className="grid grid-cols-1 gap-4 sm:grid-cols-2 lg:grid-cols-3">
            {items.map((o) => <OpportunityCard key={o.oppId} opp={o} />)}
          </div>}
    </div>
  )
}
