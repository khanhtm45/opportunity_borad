import { useState } from 'react'
import { CATEGORY_LABELS, WORKTYPE_LABELS, LOCATION_LABELS } from '../lib/constants.js'

const SORTS = [
  { v: 'newest', l: 'Mới nhất' },
  { v: 'deadline', l: 'Sắp hết hạn' },
  { v: 'popular', l: 'Phổ biến' },
]

export default function SearchFilter({ onSearch }) {
  const [q, setQ] = useState('')
  const [category, setCategory] = useState('')
  const [orgName, setOrgName] = useState('')
  const [location, setLocation] = useState('')
  const [deadline, setDeadline] = useState('')

  // Quick category tags
  const toggleQuick = (c) => setCategory((prev) => (prev === c ? '' : c))

  const submit = (e) => {
    e.preventDefault()
    onSearch({
      q,
      categories: category ? [category] : [],
      orgName,
      location,
      // "Hạn nộp" ánh xạ sang sort sắp hết hạn khi chọn
      sort: deadline === 'soon' ? 'deadline' : 'newest',
    })
  }

  return (
    <form onSubmit={submit} className="rounded-2xl border border-white/20 bg-white/10 p-4 backdrop-blur-md">
      <div className="flex flex-col gap-3 md:flex-row md:items-center">
        <input
          className="input-base md:flex-1 bg-white/90"
          placeholder="Tìm kiếm cơ hội (tên, tổ chức, từ khóa)…"
          value={q}
          onChange={(e) => setQ(e.target.value)}
        />
        <select className="input-base md:w-44 bg-white/90" value={category} onChange={(e) => setCategory(e.target.value)}>
          <option value="">Loại cơ hội</option>
          {Object.entries(CATEGORY_LABELS).map(([k, v]) => <option key={k} value={k}>{v}</option>)}
        </select>
        <input
          className="input-base md:w-44 bg-white/90"
          placeholder="Tổ chức"
          value={orgName}
          onChange={(e) => setOrgName(e.target.value)}
        />
        <select className="input-base md:w-40 bg-white/90" value={location} onChange={(e) => setLocation(e.target.value)}>
          <option value="">Địa điểm</option>
          {Object.entries(LOCATION_LABELS).map(([k, v]) => <option key={k} value={k}>{v}</option>)}
        </select>
        <select className="input-base md:w-36 bg-white/90" value={deadline} onChange={(e) => setDeadline(e.target.value)}>
          <option value="">Hạn nộp</option>
          <option value="soon">Sắp hết hạn</option>
          <option value="open">Đang mở</option>
        </select>
        <button type="submit" className="btn-primary md:w-28">Tìm</button>
      </div>

      {/* Quick filter tags */}
      <div className="mt-3 flex flex-wrap gap-2">
        {Object.entries(CATEGORY_LABELS).map(([k, v]) => (
          <button
            type="button"
            key={k}
            onClick={() => toggleQuick(k)}
            className={`chip border transition ${
              category === k
                ? 'border-brand-400 bg-brand-500 text-white'
                : 'border-white/40 bg-white/10 text-white/90 hover:border-white/70'
            }`}
          >
            {v}
          </button>
        ))}
      </div>
    </form>
  )
}
