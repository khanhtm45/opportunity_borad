// Hằng số hiển thị ánh xạ từ API enum
export const CATEGORY_LABELS = {
  INTERNSHIP: 'Thực tập',
  STARTUP_RECRUITMENT: 'Tuyển dụng Startup',
  INNOVATION_CONTEST: 'Cuộc thi Đổi mới Sáng tạo',
  HACKATHON: 'Hackathon',
  SCHOLARSHIP: 'Học bổng',
  INVESTMENT_FUND: 'Quỹ đầu tư',
  INCUBATOR: 'Ươm tạo',
}
export const WORKTYPE_LABELS = {
  ONLINE: 'Online',
  OFFLINE: 'Offline',
  HYBRID: 'Hybrid',
}
export const LOCATION_LABELS = {
  TOAN_QUOC: 'Toàn quốc',
  MIEN_BAC: 'Miền Bắc',
  MIEN_TRUNG: 'Miền Trung',
  MIEN_NAM: 'Miền Nam',
  QUOC_TE: 'Quốc tế',
}
export const STATUS_LABELS = {
  // display status (board)
  OPEN: 'Đang mở',
  CLOSING_SOON: 'Sắp đóng',
  CLOSED: 'Đã đóng',
  EXPIRED: 'Hết hạn',
  // opportunity lifecycle
  DRAFT: 'Nháp',
  PENDING: 'Chờ duyệt',
  APPROVED: 'Đã duyệt',
  HIDDEN: 'Đã ẩn',
  REJECTED: 'Từ chối',
}
export const OPP_STATUS_STYLES = {
  DRAFT: 'bg-slate-100 text-slate-600',
  PENDING: 'bg-amber-50 text-amber-700',
  APPROVED: 'bg-emerald-50 text-emerald-700',
  HIDDEN: 'bg-slate-100 text-slate-400',
  REJECTED: 'bg-rose-50 text-rose-600',
  CLOSED: 'bg-slate-100 text-slate-500',
  EXPIRED: 'bg-rose-50 text-rose-600',
}
export const APP_STATUS_LABELS = {
  SUBMITTED: 'Đã nộp',
  REVIEWING: 'Đang xét',
  INTERVIEW: 'Phỏng vấn',
  ACCEPTED: 'Trúng tuyển',
  REJECTED: 'Không trúng',
  WITHDRAWN: 'Đã rút',
}

export const CATEGORY_STYLES = {
  INTERNSHIP: 'bg-brand-50 text-brand-700',
  STARTUP_RECRUITMENT: 'bg-accent-50 text-accent-700',
  INNOVATION_CONTEST: 'bg-gold-50 text-gold-700',
  HACKATHON: 'bg-brand-50 text-brand-700',
  SCHOLARSHIP: 'bg-gold-50 text-gold-700',
  INVESTMENT_FUND: 'bg-accent-50 text-accent-700',
  INCUBATOR: 'bg-brand-50 text-brand-700',
}
export const STATUS_STYLES = {
  OPEN: 'bg-emerald-50 text-emerald-700',
  CLOSING_SOON: 'bg-gold-50 text-gold-700',
  CLOSED: 'bg-slate-100 text-slate-500',
  EXPIRED: 'bg-rose-50 text-rose-600',
}
export const APP_STATUS_STYLES = {
  SUBMITTED: 'bg-slate-100 text-slate-600',
  REVIEWING: 'bg-brand-50 text-brand-700',
  INTERVIEW: 'bg-gold-50 text-gold-700',
  ACCEPTED: 'bg-emerald-50 text-emerald-700',
  REJECTED: 'bg-rose-50 text-rose-600',
  WITHDRAWN: 'bg-slate-100 text-slate-400',
}

export function fmtDate(s) {
  if (!s) return '—'
  const d = new Date(s)
  return d.toLocaleDateString('vi-VN', { day: '2-digit', month: 'short', year: 'numeric' })
}
export function fmtDateTime(s) {
  if (!s) return '—'
  return new Date(s).toLocaleString('vi-VN', { dateStyle: 'short', timeStyle: 'short' })
}
export function daysLeft(deadline) {
  if (!deadline) return null
  const diff = new Date(deadline) - new Date()
  return Math.ceil(diff / 86400000)
}
