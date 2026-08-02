import { useEffect, useState } from 'react'
import { Link } from 'react-router-dom'
import api from '../api/client.js'
import { useAuth } from '../context/AuthContext.jsx'
import { CATEGORY_LABELS, CATEGORY_STYLES } from '../lib/constants.js'
import { InlineLoader } from '../components/Splash.jsx'
import { asset } from '../lib/assets.js'

const ACTORS = [
  {
    role: 'Sinh viên',
    code: 'STUDENT',
    points: ['Tìm & lọc 7 loại cơ hội', 'Lưu tin · nộp CV nội bộ hoặc link ngoài', 'Theo dõi trạng thái hồ sơ & thông báo'],
    cta: { to: '/board', label: 'Vào bảng tin' },
  },
  {
    role: 'Đơn vị đăng tin',
    code: 'PROVIDER',
    points: ['Tạo tin chuẩn hóa, gửi kiểm duyệt', 'Quản lý ẩn/hiện · đóng · gia hạn', 'Xử lý hồ sơ ứng tuyển'],
    cta: { to: '/provider', label: 'Cổng Provider' },
  },
  {
    role: 'Quản trị viên',
    code: 'ADMIN',
    points: ['Duyệt / từ chối tin + lý do', 'Featured & danh mục hệ thống', 'Theo dõi tương tác board'],
    cta: { to: '/admin', label: 'Cổng Admin' },
  },
]

const MODULES = [
  { id: 'F01', title: 'Bảng tin', desc: 'Card/list, featured, phân trang, trạng thái OPEN / CLOSING_SOON.' },
  { id: 'F02', title: 'Tìm & lọc', desc: 'Keyword + category, địa điểm, hình thức, deadline, lĩnh vực.' },
  { id: 'F03', title: 'Chi tiết tin', desc: 'Mô tả, yêu cầu, quyền lợi, quy trình, liên hệ org, share/view.' },
  { id: 'F04', title: 'Tương tác SV', desc: 'Apply internal/external, bookmark 24–48h, thông báo.' },
  { id: 'F05', title: 'Provider', desc: 'CRUD tin, preview, quản lý ứng tuyển.' },
  { id: 'F06', title: 'Admin', desc: 'Moderation queue, verify org, analytics cơ bản.' },
]

const STACK = [
  { t: 'Frontend', d: 'React 18 · Vite · Tailwind · Axios' },
  { t: 'Backend', d: 'Spring Boot 3 · JWT · JPA' },
  { t: 'Database', d: 'Supabase PostgreSQL · Flyway SQL' },
  { t: 'Deploy', d: 'DO App Platform · FE/BE tách app · custom domain' },
]

const DEMO = [
  { email: 'sv1@demo.ob.local', role: 'Student' },
  { email: 'provider1@demo.ob.local', role: 'Provider' },
  { email: 'admin@demo.ob.local', role: 'Admin' },
]

export default function HomePage() {
  const { user } = useAuth()
  const [featured, setFeatured] = useState([])
  const [loading, setLoading] = useState(true)

  useEffect(() => {
    api.get('/opportunities/featured').then((r) => setFeatured(r.data?.items || [])).catch(() => {}).finally(() => setLoading(false))
  }, [])

  return (
    <div className="pitch">
      {/* HERO — one composition, brand first */}
      <section className="pitch-hero relative isolate min-h-[min(88vh,720px)] overflow-hidden text-white">
        <div className="absolute inset-0 bg-brand-gradient" />
        <div
          className="absolute inset-0 opacity-30 mix-blend-soft-light"
          style={{ backgroundImage: `url(${asset('ob-network.svg')})`, backgroundSize: 'cover', backgroundPosition: 'center' }}
        />
        <div className="pointer-events-none absolute -left-24 top-10 h-72 w-72 animate-pulse-ring rounded-full bg-white/10" />
        <div className="pointer-events-none absolute -right-16 bottom-0 h-96 w-96 rounded-full bg-accent-500/20 blur-3xl" />

        <div className="relative mx-auto flex min-h-[min(88vh,720px)] max-w-6xl flex-col justify-center px-4 py-16 md:py-20">
          <p className="pitch-fade text-sm font-semibold uppercase tracking-[0.28em] text-white/80">
            Opportunity Board
          </p>
          <h1 className="pitch-fade pitch-delay-1 mt-4 max-w-3xl text-4xl font-extrabold leading-[1.1] tracking-tight md:text-6xl">
            Bảng tin cơ hội sinh viên — một nơi, đủ hành trình.
          </h1>
          <p className="pitch-fade pitch-delay-2 mt-5 max-w-xl text-base text-white/85 md:text-lg">
            Cầu nối Sinh viên · Doanh nghiệp/Startup · Admin: đăng tin có duyệt,
            lọc 7 nhóm cơ hội, ứng tuyển và theo dõi trạng thái trên một hệ thống.
          </p>
          <div className="pitch-fade pitch-delay-3 mt-8 flex flex-wrap gap-3">
            <Link to="/board" className="inline-flex items-center rounded-xl bg-white px-5 py-3 text-sm font-bold text-brand-700 shadow-card-hover transition hover:-translate-y-0.5">
              Khám phá bảng tin
            </Link>
            {!user ? (
              <Link to="/login" className="inline-flex items-center rounded-xl border border-white/40 bg-white/10 px-5 py-3 text-sm font-semibold text-white backdrop-blur transition hover:bg-white/20">
                Đăng nhập demo
              </Link>
            ) : (
              <Link to="/me" className="inline-flex items-center rounded-xl border border-white/40 bg-white/10 px-5 py-3 text-sm font-semibold text-white backdrop-blur transition hover:bg-white/20">
                Vào trang cá nhân
              </Link>
            )}
          </div>
        </div>
      </section>

      {/* Pain → value */}
      <section className="mx-auto max-w-6xl px-4 py-16 md:py-20">
        <div className="grid gap-10 md:grid-cols-2 md:gap-16">
          <div>
            <h2 className="text-2xl font-extrabold tracking-tight text-slate-900 md:text-3xl">Vấn đề</h2>
            <p className="mt-4 text-slate-600 leading-relaxed">
              Cơ hội thực tập, hackathon, học bổng và quỹ bị phân tán trên Facebook, email, web trường và diễn đàn —
              sinh viên bỏ lỡ deadline, đơn vị đăng tin khó tiếp cận đúng đối tượng, nhà trường thiếu kênh tập trung.
            </p>
          </div>
          <div>
            <h2 className="text-2xl font-extrabold tracking-tight text-slate-900 md:text-3xl">Giải pháp</h2>
            <p className="mt-4 text-slate-600 leading-relaxed">
              Opportunity Board gom toàn bộ vòng đời tin: tạo → kiểm duyệt → hiển thị → lọc → ứng tuyển/lưu/chia sẻ →
              cập nhật trạng thái, với phân quyền rõ Student / Provider / Admin.
            </p>
          </div>
        </div>
      </section>

      {/* Actors */}
      <section className="border-y border-brand-100 bg-brand-gradient-soft">
        <div className="mx-auto max-w-6xl px-4 py-16 md:py-20">
          <h2 className="text-center text-2xl font-extrabold tracking-tight text-slate-900 md:text-3xl">Ba vai trò, một hệ thống</h2>
          <p className="mx-auto mt-3 max-w-2xl text-center text-sm text-slate-600">
            Đúng actors trong tài liệu chức năng — mỗi vai trò có cổng và luồng riêng.
          </p>
          <div className="mt-10 grid gap-6 md:grid-cols-3">
            {ACTORS.map((a) => (
              <div key={a.code} className="rounded-2xl border border-white/80 bg-white/90 p-6 shadow-card backdrop-blur transition hover:-translate-y-1 hover:shadow-card-hover">
                <p className="text-xs font-bold uppercase tracking-wider text-brand-600">{a.code}</p>
                <h3 className="mt-1 text-xl font-bold text-slate-900">{a.role}</h3>
                <ul className="mt-4 space-y-2 text-sm text-slate-600">
                  {a.points.map((p) => (
                    <li key={p} className="flex gap-2">
                      <span className="mt-1.5 h-1.5 w-1.5 shrink-0 rounded-full bg-accent-500" />
                      <span>{p}</span>
                    </li>
                  ))}
                </ul>
                <Link to={a.cta.to} className="mt-5 inline-block text-sm font-semibold text-brand-600 hover:underline">
                  {a.cta.label} →
                </Link>
              </div>
            ))}
          </div>
        </div>
      </section>

      {/* 7 categories */}
      <section className="mx-auto max-w-6xl px-4 py-16 md:py-20">
        <h2 className="text-2xl font-extrabold tracking-tight text-slate-900 md:text-3xl">7 nhóm cơ hội chuẩn hệ thống</h2>
        <p className="mt-3 max-w-2xl text-sm text-slate-600">
          Danh mục cố định (is_system): từ thực tập đến ươm tạo — khớp filter F02 và seed DB.
        </p>
        <div className="mt-8 flex flex-wrap gap-3">
          {Object.entries(CATEGORY_LABELS).map(([code, label], i) => (
            <Link
              key={code}
              to={`/explore?cat=${code}`}
              className={`chip border border-transparent px-4 py-2 text-sm shadow-sm transition hover:-translate-y-0.5 ${CATEGORY_STYLES[code] || 'bg-slate-100'}`}
              style={{ animationDelay: `${i * 60}ms` }}
            >
              {label}
            </Link>
          ))}
        </div>
      </section>

      {/* Modules F01–F06 */}
      <section className="bg-slate-900 text-white">
        <div className="mx-auto max-w-6xl px-4 py-16 md:py-20">
          <h2 className="text-2xl font-extrabold tracking-tight md:text-3xl">Phạm vi chức năng F01–F06</h2>
          <p className="mt-3 max-w-2xl text-sm text-slate-300">
            Đã triển khai theo breakdown sản phẩm: board, search, detail, engagement, provider portal, moderation.
          </p>
          <div className="mt-10 grid gap-4 sm:grid-cols-2 lg:grid-cols-3">
            {MODULES.map((m) => (
              <div key={m.id} className="rounded-2xl border border-white/10 bg-white/5 p-5 transition hover:border-accent-400/50 hover:bg-white/10">
                <p className="text-xs font-bold text-accent-400">{m.id}</p>
                <h3 className="mt-1 text-lg font-bold">{m.title}</h3>
                <p className="mt-2 text-sm text-slate-300">{m.desc}</p>
              </div>
            ))}
          </div>
        </div>
      </section>

      {/* Workflows */}
      <section className="mx-auto max-w-6xl px-4 py-16 md:py-20">
        <h2 className="text-2xl font-extrabold tracking-tight text-slate-900 md:text-3xl">Hai quy trình lõi</h2>
        <div className="mt-10 grid gap-8 md:grid-cols-2">
          <ol className="space-y-4">
            <li className="text-sm font-bold uppercase tracking-wider text-brand-600">Đăng tin & duyệt</li>
            {['Provider tạo tin → Pending', 'Admin duyệt / từ chối + lý do', 'Approved hiện trên Board (+ Featured)'].map((s, i) => (
              <li key={s} className="flex items-start gap-3 text-slate-700">
                <span className="flex h-7 w-7 shrink-0 items-center justify-center rounded-full bg-brand-500 text-xs font-bold text-white">{i + 1}</span>
                <span className="pt-1 text-sm md:text-base">{s}</span>
              </li>
            ))}
          </ol>
          <ol className="space-y-4">
            <li className="text-sm font-bold uppercase tracking-wider text-accent-600">Sinh viên ứng tuyển</li>
            {['Browse / filter / detail', 'Lưu tin hoặc Apply (internal CV / external link)', 'Theo dõi status · nhận thông báo'].map((s, i) => (
              <li key={s} className="flex items-start gap-3 text-slate-700">
                <span className="flex h-7 w-7 shrink-0 items-center justify-center rounded-full bg-accent-500 text-xs font-bold text-white">{i + 1}</span>
                <span className="pt-1 text-sm md:text-base">{s}</span>
              </li>
            ))}
          </ol>
        </div>
      </section>

      {/* Stack */}
      <section className="border-y border-slate-200 bg-white">
        <div className="mx-auto max-w-6xl px-4 py-14">
          <h2 className="text-center text-xl font-extrabold text-slate-900 md:text-2xl">Kiến trúc đã vận hành</h2>
          <div className="mt-8 grid gap-4 sm:grid-cols-2 lg:grid-cols-4">
            {STACK.map((s) => (
              <div key={s.t} className="rounded-xl border border-slate-100 bg-slate-50 px-4 py-5 text-center">
                <p className="text-xs font-bold uppercase tracking-wider text-brand-600">{s.t}</p>
                <p className="mt-2 text-sm font-medium text-slate-700">{s.d}</p>
              </div>
            ))}
          </div>
        </div>
      </section>

      {/* Live featured + demo */}
      <section className="mx-auto max-w-6xl px-4 py-16 md:py-20">
        <div className="flex flex-wrap items-end justify-between gap-4">
          <div>
            <h2 className="text-2xl font-extrabold tracking-tight text-slate-900">Đang chạy trên board</h2>
            <p className="mt-2 text-sm text-slate-500">Tin nổi bật từ API production (seed demo).</p>
          </div>
          <Link to="/board" className="text-sm font-semibold text-brand-600 hover:underline">Xem toàn bộ →</Link>
        </div>
        <div className="mt-6">
          {loading ? <InlineLoader /> : featured.length === 0 ? (
            <p className="rounded-2xl border border-dashed border-slate-200 py-10 text-center text-sm text-slate-400">
              Chưa có tin featured — mở Bảng tin để xem danh sách APPROVED.
            </p>
          ) : (
            <div className="grid gap-4 sm:grid-cols-2 lg:grid-cols-3">
              {featured.filter(Boolean).slice(0, 3).map((o) => (
                <Link
                  key={o.oppId}
                  to={`/opportunities/${o.slug || o.oppId}`}
                  className="rounded-2xl border border-slate-100 bg-white p-5 shadow-card transition hover:-translate-y-0.5 hover:shadow-card-hover"
                >
                  <p className="truncate font-bold text-slate-800">{o.title}</p>
                  <p className="mt-1 truncate text-xs text-slate-400">{o.orgName}</p>
                  <span className={`chip mt-3 ${CATEGORY_STYLES[o.categoryCode] || 'bg-slate-100'}`}>
                    {CATEGORY_LABELS[o.categoryCode]}
                  </span>
                </Link>
              ))}
            </div>
          )}
        </div>

        <div className="mt-14 rounded-3xl bg-brand-gradient p-8 text-white md:p-10">
          <h2 className="text-2xl font-extrabold">Pitch live — tài khoản demo</h2>
          <p className="mt-2 text-sm text-white/85">Mật khẩu chung: <span className="font-mono font-bold">password123</span></p>
          <ul className="mt-5 grid gap-2 sm:grid-cols-3">
            {DEMO.map((d) => (
              <li key={d.email} className="rounded-xl bg-white/10 px-4 py-3 text-sm backdrop-blur">
                <p className="text-xs uppercase tracking-wide text-white/70">{d.role}</p>
                <p className="mt-1 font-mono text-xs md:text-sm">{d.email}</p>
              </li>
            ))}
          </ul>
          <div className="mt-6 flex flex-wrap gap-3">
            <Link to="/login" className="rounded-xl bg-white px-5 py-2.5 text-sm font-bold text-brand-700">
              Đăng nhập ngay
            </Link>
            <Link to="/register" className="rounded-xl border border-white/40 px-5 py-2.5 text-sm font-semibold text-white">
              Tạo tài khoản mới
            </Link>
          </div>
        </div>
      </section>
    </div>
  )
}
