import { Link } from 'react-router-dom'
import {
  CATEGORY_LABELS, CATEGORY_STYLES, STATUS_LABELS, STATUS_STYLES,
  WORKTYPE_LABELS, LOCATION_LABELS, fmtDate, daysLeft,
} from '../lib/constants.js'

const ICON_CAL = 'M6.75 3v2.25M17.25 3v2.25M3 18.75V7.5a2.25 2.25 0 012.25-2.25h13.5A2.25 2.25 0 0121 7.5v11.25m-18 0A2.25 2.25 0 005.25 21h13.5A2.25 2.25 0 0021 18.75m-18 0v-7.5A2.25 2.25 0 015.25 9h13.5A2.25 2.25 0 0121 11.25v7.5'
const ICON_GLOBE = 'M12 21a9 9 0 100-18 9 9 0 000 18zM3.6 9h16.8M3.6 15h16.8M12 3c2.5 2.5 3.5 6.5 3.5 9s-1 6.5-3.5 9c-2.5-2.5-3.5-6.5-3.5-9s1-6.5 3.5-9z'
const ICON_PIN = 'M15 10.5a3 3 0 11-6 0 3 3 0 016 0zM19.5 10.5c0 7.142-7.5 11.25-7.5 11.25S4.5 17.642 4.5 10.5a7.5 7.5 0 1115 0z'
const ICON_BOOKMARK = 'M17.593 3.322c1.1.128 1.907 1.077 1.907 2.185V21L12 17.25 4.5 21V5.507c0-1.108.806-2.057 1.907-2.185a48.507 48.507 0 0111.186 0z'
const ICON_EYE = 'M2.036 12.322a1.012 1.012 0 010-.639C3.423 7.51 7.36 4.5 12 4.5c4.638 0 8.573 3.007 9.963 7.178.07.207.07.431 0 .639C20.577 16.49 16.64 19.5 12 19.5c-4.638 0-8.573-3.007-9.963-7.178z M15 12a3 3 0 11-6 0 3 3 0 016 0z'

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
            <p className="text-xs font-medium text-slate-500">{opp.orgName}</p>
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
        {opp.workType && <span className="chip bg-slate-100 text-slate-600">
          <svg className="mr-1 h-3 w-3" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.6"><path strokeLinecap="round" strokeLinejoin="round" d={ICON_GLOBE} /></svg>
          {WORKTYPE_LABELS[opp.workType]}
        </span>}
        {opp.location && <span className="chip bg-slate-100 text-slate-600">
          <svg className="mr-1 h-3 w-3" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.6"><path strokeLinecap="round" strokeLinejoin="round" d={ICON_PIN} /></svg>
          {LOCATION_LABELS[opp.location]}
        </span>}
      </div>

      <div className="mt-4 flex items-center justify-between border-t border-slate-50 pt-3 text-xs text-slate-500">
        <span className="flex items-center gap-1">
          <svg className="h-3.5 w-3.5" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.6"><path strokeLinecap="round" strokeLinejoin="round" d={ICON_CAL} /></svg>
          {fmtDate(opp.deadline)}
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

      <div className="mt-2 flex items-center justify-between text-xs text-slate-500">
        <span className="flex items-center gap-1">
          <svg className="h-3.5 w-3.5" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.6"><path strokeLinecap="round" strokeLinejoin="round" d={ICON_EYE} /></svg>
          {opp.viewCount || 0} lượt xem
        </span>
        <span className="flex items-center gap-1">
          <svg className="h-3.5 w-3.5" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.6"><path strokeLinecap="round" strokeLinejoin="round" d={ICON_BOOKMARK} /></svg>
          {opp.bookmarkCount || 0} lưu
        </span>
      </div>
    </Link>
  )
}
