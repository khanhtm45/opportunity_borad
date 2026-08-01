import { Link } from 'react-router-dom'
import {
  CATEGORY_LABELS, CATEGORY_STYLES, STATUS_LABELS, STATUS_STYLES,
  WORKTYPE_LABELS, LOCATION_LABELS, fmtDate, daysLeft,
} from '../lib/constants.js'

export default function OpportunityCard({ opp }) {
  const dl = daysLeft(opp.deadline)
  const cat = opp.categoryCode
  return (
    <Link
      to={`/opportunities/${opp.slug || opp.oppId}`}
      className="group flex flex-col rounded-2xl border border-slate-100 bg-white p-5 shadow-card transition hover:-translate-y-0.5 hover:shadow-card-hover"
    >
      <div className="mb-3 flex items-start justify-between gap-3">
        <div className="flex items-center gap-3">
          {opp.logoUrl ? (
            <img src={opp.logoUrl} alt="" className="h-11 w-11 rounded-xl object-cover" />
          ) : (
            <div className="flex h-11 w-11 items-center justify-center rounded-xl bg-brand-gradient text-sm font-bold text-white">
              {(opp.orgName || '?').charAt(0)}
            </div>
          )}
          <div>
            <p className="text-xs font-medium text-slate-400">{opp.orgName}</p>
            <h3 className="line-clamp-2 text-[15px] font-bold leading-snug text-slate-800 group-hover:text-brand-700">
              {opp.title}
            </h3>
          </div>
        </div>
        {opp.isFeatured && (
          <span className="chip bg-gold-50 text-gold-700 shrink-0">🔥 Nổi bật</span>
        )}
      </div>

      <div className="mt-auto flex flex-wrap items-center gap-2 text-xs">
        {cat && <span className={`chip ${CATEGORY_STYLES[cat] || 'bg-slate-100 text-slate-600'}`}>
          {CATEGORY_LABELS[cat] || cat}
        </span>}
        {opp.workType && <span className="chip bg-slate-100 text-slate-600">🌐 {WORKTYPE_LABELS[opp.workType]}</span>}
        {opp.location && <span className="chip bg-slate-100 text-slate-600">📍 {LOCATION_LABELS[opp.location]}</span>}
      </div>

      <div className="mt-4 flex items-center justify-between border-t border-slate-50 pt-3 text-xs text-slate-500">
        <span>
          📅 {fmtDate(opp.deadline)}
          {dl != null && dl >= 0 && (
            <span className={dl <= 7 ? 'ml-1 text-gold-700 font-semibold' : 'ml-1'}>
              · còn {dl} ngày
            </span>
          )}
        </span>
        <span className={`chip ${STATUS_STYLES[opp.displayStatus] || 'bg-slate-100 text-slate-500'}`}>
          {STATUS_LABELS[opp.displayStatus] || opp.displayStatus}
        </span>
      </div>

      <div className="mt-2 flex items-center justify-between text-[11px] text-slate-400">
        <span>👁 {opp.viewCount || 0} lượt xem</span>
        <span>🔖 {opp.bookmarkCount || 0} lưu</span>
      </div>
    </Link>
  )
}
