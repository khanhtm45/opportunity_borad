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
export const EMPLOYMENT_TYPE_LABELS = {
  FULL_TIME: 'Toàn thời gian',
  PART_TIME: 'Bán thời gian',
  CONTRACT: 'Hợp đồng',
  FREELANCE: 'Freelance',
  OTHER: 'Khác',
}
export const JOB_LEVEL_LABELS = {
  INTERN: 'Thực tập sinh',
  STAFF: 'Nhân viên',
  TEAM_LEAD: 'Trưởng nhóm',
  MANAGER: 'Trưởng/Phó phòng',
  DIRECTOR: 'Giám đốc',
  OTHER: 'Khác',
}
export const EXPERIENCE_LEVEL_LABELS = {
  NONE: 'Không yêu cầu',
  UNDER_ONE_YEAR: 'Dưới 1 năm',
  ONE_TO_TWO: '1–2 năm',
  TWO_TO_THREE: '2–3 năm',
  THREE_TO_FIVE: '3–5 năm',
  FIVE_PLUS: 'Trên 5 năm',
}
export const EDUCATION_LEVEL_LABELS = {
  NONE: 'Không yêu cầu',
  HIGH_SCHOOL: 'THPT',
  INTERMEDIATE: 'Trung cấp',
  COLLEGE: 'Cao đẳng',
  UNIVERSITY: 'Đại học',
  POSTGRAD: 'Sau đại học',
}
export const COMPANY_SIZE_LABELS = {
  SIZE_1_10: '1–10 nhân sự',
  SIZE_11_50: '11–50 nhân sự',
  SIZE_51_200: '51–200 nhân sự',
  SIZE_201_500: '201–500 nhân sự',
  SIZE_500_PLUS: 'Trên 500 nhân sự',
  UNKNOWN: 'Chưa xác định',
}
// Khớp backend LocationType enum
export const LOCATION_LABELS = {
  HA_NOI: 'Hà Nội',
  TP_HCM: 'TP.HCM',
  DA_NANG: 'Đà Nẵng',
  TOAN_QUOC: 'Toàn quốc',
  QUOC_TE: 'Quốc tế',
  KHAC: 'Khác',
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
