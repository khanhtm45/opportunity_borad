// Splash / Loading screen — nền mạng lưới (network) + logo OB xoay
export default function Splash({ message = 'Đang tải…' }) {
  return (
    <div
      className="fixed inset-0 z-50 flex flex-col items-center justify-center bg-cover bg-center"
      style={{ backgroundImage: 'url(/network-bg.svg)' }}
    >
      <div className="absolute inset-0 bg-slate-950/10" />
      <div className="relative flex h-28 w-28 items-center justify-center">
        {/* pulse ring */}
        <span className="absolute inline-flex h-20 w-20 rounded-full bg-white/30 animate-pulse-ring" />
        {/* logo xoay */}
        <img
          src="/logo.png"
          alt="Opportunity Board"
          className="relative h-24 w-24 animate-spin-slow [animation-duration:2.4s] drop-shadow-[0_4px_12px_rgba(0,0,0,0.35)]"
        />
      </div>
      <p className="relative mt-6 text-sm font-medium text-white animate-fade-in drop-shadow">{message}</p>
    </div>
  )
}

// Spinner nhỏ dùng trong nút / inline
export function InlineLoader({ label }) {
  return (
    <span className="inline-flex items-center gap-2 text-brand-600">
      <img src="/loading.png" alt="" className="h-5 w-5 animate-spin-slow [animation-duration:1s]" />
      {label && <span className="text-sm">{label}</span>}
    </span>
  )
}
