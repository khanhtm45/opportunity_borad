# KIỂM TRA CHUYÊN SÂU 5 ĐIỂM RỦI RO
## Opportunity Board — Auth / State Machine / Featured / Fields / Notification

> Tài liệu nguồn: `Phan_Tich_Chuc_Nang_Opportunity_Board.md`
> Mục tiêu: Rà soát kỹ 5 lỗ hổng cụ thể mà tài liệu bỏ ngỏ: (1) Xác thực, (2) State machine ứng tuyển nội bộ, (3) Featured/HOT ai set, (4) Thiếu trường dữ liệu, (5) Notification chưa có entity & preference.

---

## 1. LỖ HỔNG AUTH (XÁC THỰC & PHÂN QUYỀN)

Tài liệu định nghĩa 3 role (Student / Provider / Admin) ở Mục 2 nhưng **hoàn toàn không có phân hệ Auth**. Đây là lỗ hổng lớn nhất vì mọi workflow (5.1, 5.2) đều giả định "đã đăng nhập".

### 1.1. Các bề mặt tấn công cụ thể (Attack Surface)

| # | Rủi ro | Mô tả | Mức độ |
|---|--------|-------|--------|
| A1 | Không có luồng đăng nhập/đăng ký | Không quy định password policy, session/token, refresh, logout. Hệ thống "vô hình" về mặt auth. | CRITICAL |
| A2 | Tự đăng ký role Provider | Nếu đăng ký cho phép chọn role tự do → bất kỳ ai cũng thành Provider và đăng tin. Sinh viên thành mồi cho lừa đảo thực tập/học bổng giả. | CRITICAL |
| A3 | Thiếu xác minh doanh nghiệp | `Organizations.verified_status` tồn tại nhưng không có quy trình cấp. Không verify → giả mạo công ty (brand impersonation). | CRITICAL |
| A4 | Privilege escalation (IDOR/BOLA) | Nếu role lưu ở client hoặc token không được validate server-side → sửa token thành Admin. | HIGH |
| A5 | Không có MFA cho Admin | Admin là quyền cao nhất (duyệt tin, xoá user) nhưng không quy định MFA bắt buộc. | HIGH |
| A6 | Brute-force / Account takeover | Không có rate-limit đăng nhập, không khóa tài khoản sau N lần sai, không có luồng quên mật khẩu an toàn. | HIGH |
| A7 | Session management | Không có timeout, concurrent session limit, thu hồi token khi đổi mật khẩu. | MEDIUM |
| A8 | Dữ liệu nhạy cảm gắn với auth | CV, info sinh viên phụ thuộc auth; auth yếu = leak PDPD. | HIGH |

### 1.2. Thiếu sót kiểm soát (Checklist phải có)

- [ ] Password policy (độ dài, complexity, không trùng lịch sử).
- [ ] Hash + salt (bcrypt/argon2) — field `password_hash` chưa có trong `Users`.
- [ ] Session hoặc JWT: expiry, rotation, revocation (blacklist/version).
- [ ] Đăng nhập: rate-limit + CAPTCHA + lockout sau 5 lần sai.
- [ ] Quên/mật khẩu: token hết hạn 15p, gửi qua email xác thực.
- [ ] Xác thực email / SSO trường (nếu bắt buộc dùng email trường).
- [ ] Phân quyền: ma trận RBAC rõ ràng (xem Mục 5 bên dưới & file riêng).
- [ ] Provider onboarding: đăng ký → nộp giấy tờ → Admin duyệt → `verified_status=true`.
- [ ] MFA bắt buộc cho Admin & Provider.
- [ ] Audit log mọi thay đổi quyền (ai cấp role, khi nào).

### 1.3. Quy trình cấp quyền đề xuất (Role Assignment)

```
Student:  tự đăng ký (email verified) ───────────────► role=Student
Provider: đăng ký ─► nộp thông tin tổ chức ─► Admin duyệt ─► verified_status=Pending→Verified
Admin:    CHỈ Admin tạo (không tự đăng ký) ──────────► role=Admin, bắt buộc MFA
```

> Quy tắc: role KHÔNG được chọn tự do lúc đăng ký. Sinh viên mặc định; Provider phải qua duyệt; Admin chỉ do Admin cấp.

---

## 2. STATE MACHINE ỨNG TUYỂN NỘI BỘ (Apply Internal)

Tài liệu mô tả 2 nơi không khớp nhau:
- F04.1 (chức năng): Đã nộp / Đang xem xét / Trúng tuyển-Mời phỏng vấn / Từ chối.
- Mục 7 (`Applications.status`): Submitted, Reviewed, Accepted, Rejected — **thiếu Interview**.

### 2.1. Các trạng thái chuẩn hóa (đề xuất)

| Mã lưu | Nhãn hiển thị | Ý nghĩa |
|--------|---------------|---------|
| SUBMITTED | Đã nộp | SV gửi CV, chờ Provider xử lý |
| REVIEWING | Đang xem xét | Provider đã mở/chấp nhận xem |
| INTERVIEW | Mời phỏng vấn | Provider mời (tương ứng "Trúng tuyển-Mời phỏng vấn") |
| ACCEPTED | Trúng tuyển | Nhận chính thức |
| REJECTED | Từ chối | Không qua |

> Sửa: bỏ nhãn "Reviewed" gây nhầm ("đã xem xét" ≠ "đang xem xét"). Dùng REVIEWING. Thêm INTERVIEW.

### 2.2. Bảng chuyển trạng thái (Transition Table)

```
           Actor: chỉ Provider sở hữu opp (hoặc Admin hệ thống trong trường hợp force)
           Sinh viên KHÔNG được tự đổi trạng thái của mình.

 SUBMITTED ──(Provider bắt đầu xem)──► REVIEWING
 SUBMITTED ──(Provider từ chối ngay)─► REJECTED
 REVIEWING ──(mời phỏng vấn)─────────► INTERVIEW
 REVIEWING ──(từ chối)───────────────► REJECTED
 INTERVIEW ──(nhận chính thức)──────► ACCEPTED
 INTERVIEW ──(từ chối sau PV)───────► REJECTED

 Quy tắc:
  - Chỉ tiến (forward), KHÔNG lùi (INTERVIEW không quay lại REVIEWING).
  - Mỗi chuyển đổi ghi: updated_at + updated_by + note (lý do nếu REJECTED).
  - Hủy ứng tuyển: SV chỉ rút được khi status = SUBMITTED hoặc REVIEWING (tạo trạng thái WITHDRAWN).
```

### 2.3. Các lỗ hổng của state machine hiện tại

| # | Vấn đề | Hậu quả | Sửa |
|---|--------|---------|-----|
| S1 | Thiếu INTERVIEW | Không thể hiện "Mời phỏng vấn" | Thêm trạng thái |
| S2 | Không quy định ai chuyển trạng thái | SV hoặc kẻ lạ sửa status | Chỉ Provider (owner) |
| S3 | Chỉ có `applied_at`, thiếu mốc thời gian其余 | Không đo được thời gian xử lý | Thêm reviewed_at, interviewed_at, decided_at |
| S4 | Thiếu trường ghi chú/từ chối | Provider không phản hồi được SV | Thêm `provider_note`, `rejection_reason` |
| S5 | Không chặn ứng tuyển trùng | SV nộp nhiều lần 1 opp | Unique(student_id, opp_id) |
| S6 | Ứng tuyển sau deadline | Vượt hạn nộp | Backend chặn nếu opp hết hạn |
| S7 | opp Closed/Rejected khi app đang chạy | App treo vô nghĩa | Khi opp đóng → app auto chuyển CLOSED_BY_OWNER hoặc ẩn |
| S8 | opp dùng external_link → không có app record | `apply_count` đếm sai, state machine vô dụng | Quy ước: app nội bộ chỉ áp dụng khi `is_external=false` (xem Mục 4) |

### 2.4. Trường bổ sung cho `Applications`

```
Applications (
  app_id, opp_id, student_id,
  cv_file, cover_letter,
  status,                 -- SUBMITTED/REVIEWING/INTERVIEW/ACCEPTED/REJECTED/WITHDRAWN
  is_external,            -- mirror từ opp
  provider_note,          -- phản hồi Provider
  rejection_reason,       -- lý do từ chối
  applied_at,
  reviewed_at, interviewed_at, decided_at, updated_at,
  updated_by              -- user_id Provider thực hiện
)
UNIQUE(student_id, opp_id)   -- chặn trùng
```

---

## 3. FEATURED / HOT — AI SET?

F01.2: "Khu vực Banner Top / Slider dành cho các cơ hội HOT, tài trợ hoặc ưu tiên đẩy tin."

Tài liệu **không nói ai set, bằng cơ chế nào, và không có trường lưu**. Đây là gap cả chức năng lẫn dữ liệu.

### 3.1. Các kịch bản (chưa được chọn)

| Cách | Ai set | Rủi ro nếu không quy định |
|------|--------|---------------------------|
| Admin thủ công | Admin chọn tay | Cần action + audit; nếu không có trường thì không lưu được |
| Provider trả phí (boost) | Provider mua | Cần tích hợp Payment (mục 6 nhắc tương lai) + chống lạm dụng |
| Thuật toán | Hệ thống auto (top view/apply) | Cần logic ranking, có thể đẩy tin lừa đảo nếu lượt giả |
| Kết hợp | Admin duyệt + Provider đề xuất | Khuyên dùng: Provider đề xuất → Admin duyệt |

### 3.2. Yêu cầu tối thiểu phải có

- Trường lưu: `is_featured` (bool), `featured_by` (user_id Admin), `featured_at`, `featured_until` (hết hạn tự rớt).
- Featured phải tự động rớt xuống khi opp chuyển sang Closed/Rejected/quá `featured_until`.
- Audit log: ai featured cái gì, khi nào (chống thao túng/ưu ái).
- Nếu mở boost trả phí: cần Payment + quota (1 opp chỉ boost N ngày).

### 3.3. Đề xuất

```
is_featured  BOOLEAN DEFAULT false
featured_by  user_id NULL
featured_at  TIMESTAMP NULL
featured_until TIMESTAMP NULL   -- auto drop khi quá hạn hoặc opp đóng

Quy trình: Provider đề xuất "Đẩy tin" ─► Admin duyệt ─► set is_featured + featured_until
```

---

## 4. CÁC TRƯỜNG DỮ LIỆU THIẾU (Fields Gap)

### 4.1. `Opportunities` — bổ sung

| Trường | Bắt buộc? | Lý do |
|--------|-----------|-------|
| `is_external` | BẮT BUỘC | Phân biệt nộp CV nội bộ vs link ngoài (giải quyết S8, F04.1) |
| `is_featured`, `featured_by`, `featured_at`, `featured_until` | BẮT BUỘC | Mục 3 |
| `view_count`, `apply_count` | BẮT BUỘC | F06.3 thống kê & ranking (không tính aggregate mỗi lần) |
| `slug` | NÊN | URL thân thiện cho chia sẻ (F04.3) |
| `published_at` | NÊN | Thời điểm Approved→lên bảng (F02.3 sort "mới nhất") |
| `rejection_reason` | NÊN | Admin từ chối → gửi lý do (5.1 workflow) |
| `moderated_by`, `moderated_at` | NÊN | Audit kiểm duyệt |
| `contact_email`, `contact_phone` | NÊN | Thông tin liên hệ trực tiếp |
| `max_applicants` | TÙY | Giới hạn slot (hackathon/scholarship) |
| `domain_ids` (FK → Domains) | BẮT BUỘC | Lĩnh vực để lọc F02.2 & notification |
| `created_by` | NÊN | User trong org đã đăng |

### 4.2. `Applications` — bổ sung (đã liệt ở 2.4)

SUBMITTED/REVIEWING/INTERVIEW/ACCEPTED/REJECTED/WITHDRAWN + `is_external`, `provider_note`, `rejection_reason`, `reviewed_at`, `interviewed_at`, `decided_at`, `updated_at`, `updated_by`, UNIQUE(student_id, opp_id).

### 4.3. Entity hoàn toàn thiếu

| Entity | Dùng cho |
|--------|----------|
| `Domains` (IT, Marketing, Tài chính...) | F02.2 lọc lĩnh vực, F06.2 tag, notification preference |
| `OpportunityDomains` (junction) | Quan hệ N-N opp–domain |
| `Notifications` | Hệ thống thông báo (Mục 5) |
| `NotificationPreferences` | Tùy chọn nhận thông báo |
| `DeviceTokens` | Push notification |
| `AuditLogs` | Kiểm duyệt, featured, đổi quyền |
| `SavedSearches` (tùy) | Lưu bộ lọc yêu thích |

---

## 5. NOTIFICATION — CHƯA CÓ ENTITY & PREFERENCE

Tài liệu nhắc thông báo ở nhiều chỗ (F04.2 alert hết hạn 24-48h; F04.3 email/push opp mới theo lĩnh vực; Admin nhận báo tin chờ duyệt 5.1) nhưng **không có entity nào** ở Mục 7 và **không có preference**.

### 5.1. Các trigger thông báo xác định từ tài liệu

| Mã | Trigger | Đối tượng | Kênh |
|----|---------|-----------|------|
| N1 | Opp mới được duyệt thuộc lĩnh vực SV quan tâm | Student | Email/Push |
| N2 | Opp SV đã lưu sắp hết hạn (24-48h) | Student | Email/Push |
| N3 | Trạng thái ứng tuyển thay đổi (REVIEWING/INTERVIEW/ACCEPTED/REJECTED) | Student | Email/Push |
| N4 | Có opp mới chờ duyệt | Admin | Email/In-app |
| N5 | Opp bị từ chối (gửi lý do) | Provider | Email |
| N6 | Opp được duyệt / featured | Provider | Email |

### 5.2. Entity đề xuất

```
Notifications (
  notification_id,
  user_id,
  type,              -- NEW_OPP / DEADLINE_ALERT / APP_STATUS / PENDING_REVIEW / OPP_REJECTED / OPP_APPROVED
  channel,           -- EMAIL / PUSH / IN_APP
  title, body,
  ref_id,            -- opp_id hoặc app_id liên quan
  is_read BOOLEAN,
  sent_at TIMESTAMP,
  created_at TIMESTAMP
)

NotificationPreferences (
  user_id,
  type,              -- theo mã N1..N6
  channel,           -- EMAIL / PUSH
  enabled BOOLEAN,
  frequency,         -- INSTANT / DAILY_DIGEST / WEEKLY
  categories JSON,   -- mảng category_id quan tâm (N1)
  domains JSON       -- mảng domain_id quan tâm (N1)
)

DeviceTokens (
  user_id,
  token,
  platform,          -- IOS / ANDROID / WEB
  updated_at
)
```

### 5.3. Preference model (điểm tài liệu bỏ ngỏ)

- SV chọn: loại thông báo nào (N1–N3), kênh nào (email/push), tần suất (tức thì / gộp ngày / tuần).
- SV chọn lĩnh vực & loại cơ hội quan tâm → dùng cho N1 (opp mới).
- Cơ chế **unsubscribe** bắt buộc (pháp lý + trải nghiệm).
- Digest: gom nhiều N1 trong ngày gửi 1 mail → tránh spam.

### 5.4. Delivery & scheduler (cần tác vụ nền)

- **Worker gộp N1:** khi opp Approved → quét `NotificationPreferences` khớp category/domain → tạo bản ghi `Notifications` → queue gửi.
- **Scheduler N2:** job chạy hàng giờ quét opp có `deadline` trong [24h,48h] và có `SavedOpportunities` → tạo alert.
- **Trigger N3/N4/N5/N6:** sự kiện đổi trạng thái → publish event → consumer tạo notification.
- **Tracking:** trạng thái sent/failed/read; retry khi gửi fail.

---

## 6. TỔNG KẾT & KHUYẾN NGHỊ

| Điểm | Trạng thái | Hành động ngay |
|------|-----------|----------------|
| Auth | Vắng mặt hoàn toàn | Thiết kế Auth + RBAC + Provider verification (CRITICAL) |
| State machine Apply | Thiếu INTERVIEW, thiếu quy tắc chuyển, thiếu trường | Chuẩn hóa 6 trạng thái + transition table + trường bổ sung |
| Featured/HOT | Không có trường, không ai chịu trách nhiệm | Thêm `is_featured*` + quy trình Admin duyệt boost |
| Fields | Thiếu `is_external`, counters, domains, audit | Bổ sung theo Mục 4 |
| Notification | Không entity, không preference | Thêm 3 entity + worker/scheduler |

> Ba điểm CRITICAL cần đóng trước khi viết bất kỳ code nào: (1) Auth, (2) `is_external` + state machine, (3) Notification entity & preference. Thiếu chúng hệ thống không thể vận hành an toàn và thống kê sẽ sai.

---

*Tài liệu phân tích chuyên sâu — bổ sung cho `Phan_Tich_Phan_Bien_Opportunity_Board.md`.*
