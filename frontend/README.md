# FRONTEND — OPPORTUNITY BOARD
## React + Vite + Tailwind CSS (dựa trên phân tích màu logo & loading)

### Phân tích ảnh (assets gốc: `image/logo/logo.png`, `image/loading/loading.png`)
Dùng script `analyze_colors.py` (PIL + numpy) trích xuất palette:
- **Logo (OB mark):** gradient xanh dương `#0388ED` → vàng cam `#F69022`, các điểm sáng.
- **Loading (OppHub):** "Opp" xanh `#0388ED`, "Hub" cam `#F69022`, navy `#0736AC`, gold `#FCBD0E`.

→ Palette áp dụng vào Tailwind theme (`tailwind.config.js`):
- `brand` (xanh dương, primary, shades 50–950, base `#0388ED`)
- `accent` (cam, `#F69022`)
- `gold` (vàng điểm nhấn, `#FCBD0E`)
- gradient `brand-gradient` (xanh→navy→cam) dùng cho hero & splash.

### Cấu trúc
```
frontend/
├── index.html                (favicon logo, font Inter)
├── tailwind.config.js        (theme từ palette ảnh)
├── postcss.config.js
├── vite.config.js            (dev proxy /api -> :8080)
├── public/logo.png, loading.png
└── src/
    ├── main.jsx, App.jsx, index.css
    ├── api/client.js         (axios + JWT interceptor + auto-refresh)
    ├── context/AuthContext.jsx
    ├── lib/constants.js      (map enum -> label tiếng Việt)
    ├── components/  Splash, Navbar, Layout, OpportunityCard, SearchFilter
    └── pages/  BoardPage, DetailPage, LoginPage, RegisterPage,
                BookmarksPage, ProviderPage, AdminPage
```

### Chức năng đã implemented (khớp API_SPEC)
- **F01 Board:** list công khai + featured slider + card.
- **F02 Search/Filter:** keyword + category (chips) + work_type + location + sort. Đã verify thực tế trên Supabase.
- **F03 Detail:** mô tả/yêu cầu/quyền lợi + related + bookmark/apply (EXTERNAL → link ngoài).
- **F04 Student:** bookmark (toggle), apply internal, my bookmarks.
- **Auth:** register/login (JWT), role-based redirect, refresh token.
- **F05 Provider:** tạo + submit duyệt (state DRAFT→PENDING), danh sách opp.
- **F06 Admin:** moderation queue approve/reject/feature.
- **Splash/Loading:** logo xoay + pulse-ring, gradient brand (từ ảnh loading).

### Chạy local
```bash
npm install
# backend phải chạy ở :8080 (hoặc sửa proxy vite.config.js)
npm run dev      # http://localhost:5173  (proxy /api -> :8080)
npm run build    # dist/ production
```

### Deploy
Push `main`:
- **FE → GitHub Pages:** `https://khanhtm45.github.io/opportunity_borad/` ([`deploy.yml`](../.github/workflows/deploy.yml))
- **DO = 2 app riêng:** [`.do/app-api.yaml`](../.do/app-api.yaml) (BE / oyster) + [`.do/app-frontend.yaml`](../.do/app-frontend.yaml) (FE / shark)
- **BE artifact → GitHub Releases:** fat JAR `backend-<run_number>`

Setup:
1. DO **API app**: source `backend/`, Dockerfile `Dockerfile`, route `/api`, secrets `JWT_SECRET`, `DB_PASSWORD` → `https://oyster-app-eleoo.ondigitalocean.app/api/v1`
2. DO **FE app**: source `frontend/`, `VITE_BASE_PATH=/`, `VITE_API_URL=https://oyster-app-eleoo.ondigitalocean.app/api/v1` → Force Rebuild
3. GitHub **Variables** `VITE_API_URL` = URL API ở bước 1 → Deploy Pages; Pages Source = GitHub Actions.

Local: không set `VITE_API_URL` → `/api/v1` + Vite proxy `:8080`.

### Lưu ý
- Proxy dev đã cấu hình forward `/api` tới backend Spring Boot.
- `vite.config.js` dùng `base: '/opportunity_borad/'` cho GitHub Pages; `postbuild` copy `404.html` (SPA fallback).
- Màu sắc đồng bộ với brand: primary xanh `#0388ED`, accent cam `#F69022`.
