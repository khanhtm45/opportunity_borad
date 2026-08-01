import { useState } from 'react'
import { CATEGORY_LABELS, WORKTYPE_LABELS, LOCATION_LABELS } from '../lib/constants.js'

const SORTS = [
  { v: 'newest', l: 'Mới nhất' },
  { v: 'deadline', l: 'Sắp hết hạn' },
  { v: 'popular', l: 'Phổ biến' },
]

export default function SearchFilter({ onSearch }) {
  const [q, setQ] = useState('')
  const [categories, setCategories] = useState([])
  const [workType, setWorkType] = useState('')
  const [location, setLocation] = useState('')
  const [sort, setSort] = useState('newest')

  const toggleCat = (c) =>
    setCategories((prev) => (prev.includes(c) ? prev.filter((x) => x !== c) : [...prev, c]))

  const submit = (e) => {
    e.preventDefault()
    onSearch({ q, categories, workType, location, sort })
  }

  return (
    <form onSubmit={submit} className="rounded-2xl border border-slate-100 bg-white p-4 shadow-card">
      <div className="flex flex-col gap-3 md:flex-row">
        <input
          className="input-base md:flex-1"
          placeholder="Tìm kiếm cơ hội (tên, tổ chức, từ khóa)…"
          value={q}
          onChange={(e) => setQ(e.target.value)}
        />
        <select className="input-base md:w-44" value={workType} onChange={(e) => setWorkType(e.target.value)}>
          <option value="">Hình thức</option>
          {Object.entries(WORKTYPE_LABELS).map(([k, v]) => <option key={k} value={k}>{v}</option>)}
        </select>
        <select className="input-base md:w-44" value={location} onChange={(e) => setLocation(e.target.value)}>
          <option value="">Khu vực</option>
          {Object.entries(LOCATION_LABELS).map(([k, v]) => <option key={k} value={k}>{v}</option>)}
        </select>
        <select className="input-base md:w-40" value={sort} onChange={(e) => setSort(e.target.value)}>
          {SORTS.map((s) => <option key={s.v} value={s.v}>{s.l}</option>)}
        </select>
        <button type="submit" className="btn-primary md:w-28">Tìm</button>
      </div>

      <div className="mt-3 flex flex-wrap gap-2">
        {Object.entries(CATEGORY_LABELS).map(([k, v]) => (
          <button
            type="button"
            key={k}
            onClick={() => toggleCat(k)}
            className={`chip border transition ${
              categories.includes(k)
                ? 'border-brand-500 bg-brand-500 text-white'
                : 'border-slate-200 bg-white text-slate-600 hover:border-brand-300'
            }`}
          >
            {v}
          </button>
        ))}
      </div>
    </form>
  )
}
