import { asset } from '../lib/assets.js'

// Nền mạng lưới (AI network) dùng chung cho toàn bộ trang
// className truyền thêm (vd flex, min-h-screen) tuỳ ngữ cảnh
export default function NetworkBg({ children, className = '', overlay = 'bg-slate-950/15' }) {
  return (
    <div
      className={`relative bg-cover bg-center ${className}`}
      style={{ backgroundImage: `url(${asset('network-bg.svg')})` }}
    >
      {overlay && <div className={`absolute inset-0 ${overlay}`} />}
      <div className="relative">{children}</div>
    </div>
  )
}
