# OPPORTUNITY BOARD — API & BACKEND DESIGN
## REST API Specification + RBAC + State Machine Enforcement

> Dựa trên: `schema.sql` (docs/db/) + phát hiện từ `Phan_Tich_Chi_Tiet_Lo_Hong.md`.
> Base URL: `/api/v1`  ·  Auth: Bearer JWT  ·  Format: JSON

**Swagger UI:** `/api/swagger-ui.html` · OpenAPI JSON: `/api/v3/api-docs`  
Production: `https://oyster-app-eleoo.ondigitalocean.app/api/swagger-ui.html`

---

## 1. CONVENTIONS

- **Auth header:** `Authorization: Bearer <JWT>` (access token). Refresh token qua cookie httpOnly.
- **Lỗi chuẩn:** `{ "error": { "code": "OPP_NOT_FOUND", "message": "...", "field": null } }`.
- **Phân trang:** cursor-based cho board (`?cursor=&limit=20`) tránh offset lag (NFR <2s, 5000+ cc).
- **Phân quyền:** mọi endpoint có guard server-side (chống IDOR/BOLA). Role lấy từ JWT, không tin client.
- **Validation:** deadline phải > now; slug tự sinh; external_link bắt buộc nếu apply_mode=EXTERNAL (check DB).

---

## 2. MA TRẬN RBAC (Role × Action)

| Chức năng | STUDENT | PROVIDER | ADMIN |
|-----------|:------:|:--------:|:-----:|
| Xem board / filter / detail | ✅ | ✅ | ✅ |
| Bookmark / Share / Nhận notif | ✅ | ❌ | ❌ |
| Apply internal (nộp CV) | ✅ | ❌ | ❌ |
| Quản lý hồ sơ của mình | ✅ | ❌ | ✅(force) |
| Tạo / sửa bài đăng (org mình) | ❌ | ✅ | ✅ |
| Xem ứng tuyển của opp mình | ❌ | ✅(owner only) | ✅ |
| Đổi status app (opp mình) | ❌ | ✅ | ✅ |
| Featured/boost (đề xuất) | ❌ | ✅(đề xuất) | ✅(duyệt + set) |
| Kiểm duyệt (approve/reject) | ❌ | ❌ | ✅ |
| Quản lý category/domain/tag | ❌ | ❌ | ✅ |
| Analytics / export | ❌ | ✅(org mình) | ✅(toàn hệ thống) |
| Quản lý user / verify provider | ❌ | ❌ | ✅ |
| Audit log | ❌ | ❌ | ✅(read) |

> Quy tắc: PROVIDER chỉ truy cập dữ liệu của `org_id` thuộc sở hữu (join org_members / owner_user_id). Mọi query ứng tuyển phải lọc qua opp→org→member.

---

## 3. API ENDPOINTS

### A. AUTH (RÀNG BUỘC A1 — vắng mặt trong tài liệu gốc)

| Method | Path | Mô tả | Role |
|--------|------|-------|------|
| POST | `/auth/register` | Đăng ký. Body: `{role, email, password, full_name}` | Public |
| POST | `/auth/login` | Đăng nhập. Rate-limit 5 lần/sai → lock 15p | Public |
| POST | `/auth/refresh` | Làm mới access token | User |
| POST | `/auth/logout` | Thu hồi token (tăng password_version) | User |
| POST | `/auth/forgot-password` | Gửi token hết hạn 15p | Public |
| POST | `/auth/reset-password` | Đặt lại mk | Public |
| POST | `/auth/verify-email` | Xác thực email | User |
| POST | `/auth/providers/register` | Đăng ký org + hồ sơ tổ chức (≥1 URL) → `verified_status=PENDING` | Public |
| GET  | `/auth/me` | Thông tin tôi + role | User |

**Quy tắc role (A1.2):**
- `role=STUDENT` → tạo luôn, cần verify email.
- `role=PROVIDER` → tạo user + org ở `verified_status=PENDING`; **bắt buộc ≥1 `org_documents`** (BUSINESS_LICENSE/TAX_CODE/IDENTITY/OTHER + `file_url`); CHỈ được đăng tin sau khi ADMIN duyệt verify.
- `role=ADMIN` → KHÔNG cho đăng ký tự do; chỉ Admin tạo + bắt buộc MFA.

**Body `/auth/providers/register` (bổ sung):**
```json
{
  "orgName": "...", "contactEmail": "...", "contactFullName": "...", "password": "...",
  "documents": [{ "docType": "BUSINESS_LICENSE", "title": "GPKD", "fileUrl": "https://..." }]
}
```

---

### B. F01 — BẢNG TIN (Board View)

| Method | Path | Mô tả | Role |
|--------|------|-------|------|
| GET | `/opportunities` | List công khai, cursor pagination, chỉ `display_status=OPEN/CLOSING_SOON` | Public |
| GET | `/opportunities/featured` | Slider/HOT (`is_featured=true` & chưa hết hạn) | Public |
| GET | `/opportunities/:slug` | Detail + related (cùng domain) | Public |
| POST | `/opportunities/:id/view` | Tăng `view_count` (idempotent theo user/IP) | User/Public |

**Response list item (card):**
```json
{
  "opp_id": "uuid", "title": "...", "org_name": "...", "logo_url": "...",
  "category_code": "HACKATHON", "display_status": "OPEN|CLOSING_SOON",
  "deadline": "2026-09-01T23:59:59Z", "work_type": "ONLINE",
  "location": "TOAN_QUOC", "is_featured": false, "bookmark_count": 12
}
```

---

### C. F02 — TÌM KIẾM & LỌC

| Method | Path | Mô tả | Role |
|--------|------|-------|------|
| GET | `/opportunities/search` | Query đa tiêu chí | Public |

**Params:** `q` (keyword full-text), `categories[]`, `work_type`, `location`, `status` (display), `domains[]`, `sort` (newest|deadline|popular), `cursor`, `limit`.
- Tìm kiếm dùng GIN `to_tsvector` (schema) thay vì `LIKE` → đạt NFR <2s.
- Sort popular = `view_count + application_count` denormalized.

---

### D. F03 — CHI TIẾT (trong F01 detail)

Đã cover ở `GET /opportunities/:slug`. Trả thêm: requirements, benefits, quy trình, info org, related list (cùng domain, limit 5).

---

### E. F04 — SINH VIÊN

| Method | Path | Mô tả | Role |
|--------|------|-------|------|
| POST | `/opportunities/:id/apply` | Nộp CV nội bộ (chỉ khi `apply_mode=INTERNAL`) | Student |
| GET  | `/opportunities/:id/external-click` | Log click + redirect (khi EXTERNAL) | Student/Public |
| POST | `/opportunities/:id/save` | Bookmark (F04.2), body `{notify_before_hours:24|48}` | Student |
| DELETE | `/opportunities/:id/save` | Bỏ bookmark | Student |
| GET  | `/me/bookmarks` | Cơ hội đã lưu | Student |
| GET  | `/me/profile` | Hồ sơ SV + `cvUrl` / `hasCv` | Student |
| PUT  | `/me/profile` | Cập nhật hồ sơ / CV URL (body: `cvUrl`, major, university, universityYear, skills[], bio) | Student |
| POST | `/me/uploads` | Upload S3 **private + AES256** (`purpose=cv\|image\|avatar\|logo\|banner\|org-doc\|opp-doc`) → `{url:ob-s3://…, viewUrl, encryptedAtRest}` | Authenticated |
| POST | `/uploads/guest` | Upload hồ sơ org khi đăng ký NTD (chỉ `org-doc`) | Public |
| GET  | `/media/view?ref&exp&sig` | Redirect presign S3 (HMAC) — xem ảnh/hồ sơ không public bucket | Public (có chữ ký) |
| GET  | `/media/access?ref` | Cấp `viewUrl` / `fetchUrl` mới | Authenticated |
| GET  | `/me/applications` | Hồ sơ mình đã nộp + status | Student |
| GET  | `/me/applications/:app_id` | Chi tiết 1 app + history | Student |
| POST | `/me/applications/:app_id/withdraw` | Rút (chỉ SUBMITTED/REVIEWING) | Student |
| POST | `/opportunities/:id/share` | Sinh deep-link tracking | Public |

**RÀNG BUỘC Apply (S6/S8):**
- Nếu `apply_mode=EXTERNAL` → `POST /apply` trả `409 CONFLICT` (dùng external-click).
- Chặn nộp sau `deadline` hoặc opp không `OPEN`.
- UNIQUE(opp_id, student_id) → trùng trả `409 ALREADY_APPLIED`.
- `cvFile` optional trên apply; nếu thiếu → lấy `student_profiles.cv_url`. Không có CV → `400` (vào `/me/profile` tải CV — URL https công khai).

---

### F. F05 — PROVIDER PORTAL

| Method | Path | Mô tả | Role |
|--------|------|-------|------|
| POST | `/opportunities` | Tạo bài đăng DRAFT; body có thể kèm `documents[]` | Provider |
| POST | `/opportunities/:id/submit` | Gửi duyệt — **bắt buộc ≥1 opportunity document** | Provider |
| PUT  | `/opportunities/:id` | Sửa DRAFT/HIDDEN; `documents` (nếu gửi) thay thế toàn bộ | Provider(owner) |
| POST | `/opportunities/:id/hide` | Ẩn/Hiện (APPROVED↔HIDDEN) | Provider(owner) |
| POST | `/opportunities/:id/close` | Đóng sớm (APPROVED→CLOSED) | Provider(owner) |
| POST | `/opportunities/:id/extend` | Gia hạn deadline | Provider(owner) |
| POST | `/opportunities/:id/feature-request` | Đề xuất boost (ADMIN duyệt) | Provider |
| GET  | `/provider/opportunities` | Danh sách opp của org mình | Provider |
| GET  | `/provider/opportunities/:id/applications` | DS ứng tuyển (opp mình) | Provider(owner) |
| GET  | `/provider/opportunities/:id/documents` | Hồ sơ liên quan tin đăng | Provider(owner) |
| GET  | `/provider/org` | Hồ sơ tổ chức + `verifiedStatus` / `verificationNote` / `needsUpdate` | Provider |
| PUT  | `/provider/org` | Cập nhật thông tin org (sai/thiếu → về `PENDING` chờ quét lại) | Provider |
| GET  | `/provider/org/documents` | DS hồ sơ tổ chức | Provider |
| POST | `/provider/org/documents` | Thêm hồ sơ org `{docType,title,fileUrl}` (reset `PENDING` nếu chưa VERIFIED) | Provider |
| DELETE | `/provider/org/documents/:docId` | Xoá hồ sơ org (chỉ khi chưa VERIFIED) | Provider |
| GET  | `/provider/applications/:app_id` | Xem 1 app + CV | Provider(owner) |
| PUT  | `/provider/applications/:app_id/status` | Đổi status (state machine; `?to=` hoặc body `{status,note}`) | Provider(owner) |
| POST | `/provider/applications/:app_id/ai-scan` | **Lớp 3** — AI quét 1 CV/hồ sơ SV (body `{criteria}`) | Provider(owner) |
| POST | `/provider/opportunities/:id/applications/ai-scan` | **Lớp 3** — AI quét hàng loạt + nhóm APPROVE/REVIEW/REJECT | Provider(owner) |
| POST | `/provider/applications/:app_id/request-update` | Gửi lý do yêu cầu SV cập nhật hồ sơ (`APP_UPDATE_REQUIRED`) | Provider(owner) |
| GET  | `/provider/applications/export?fmt=csv` | Xuất Excel/CSV | Provider(owner) |
| GET  | `/provider/stats` | Thống kê org mình | Provider |

> **Lớp 3 — Hồ sơ ứng tuyển:** Provider nhập tiêu chuẩn screening → AI quét CV (+ profile).  
> `apply=true` chỉ lưu `ai_moderation_note` / `screening_criteria` — **không tự ACCEPT/REJECT**.  
> Provider xem nhóm lý do → chỉnh → `request-update` (SV nhận `APP_UPDATE_REQUIRED`) hoặc đổi status.

**Hồ sơ tin đăng (`documents` trên create/update):** `{docType, title, fileUrl}` với `docType` = `PROGRAM_PROOF` \| `PARTNERSHIP_LETTER` \| `OTHER`.  
`submit` trả `400` nếu tin chưa có ≥1 document.

**Thuộc tính tin đăng (kiểu TopCV, optional trên create/update):**
`jobLevel` (`INTERN|STAFF|TEAM_LEAD|MANAGER|DIRECTOR|OTHER`),
`experienceLevel` (`NONE|UNDER_ONE_YEAR|ONE_TO_TWO|TWO_TO_THREE|THREE_TO_FIVE|FIVE_PLUS`),
`educationLevel` (`NONE|HIGH_SCHOOL|INTERMEDIATE|COLLEGE|UNIVERSITY|POSTGRAD`),
`employmentType` (`FULL_TIME|PART_TIME|CONTRACT|FREELANCE|OTHER`),
`headcount`, `salaryMin`, `salaryMax`, `salaryCurrency`, `salaryNegotiable`,
`addressDetail`, `workingSchedule`, `skills` (text).

**Đăng ký org bổ sung:** `contactPhone`, `taxCode`, `address`, `industry`, `companySize` (`SIZE_1_10`…`SIZE_500_PLUS`|`UNKNOWN`).

---

### G. F06 — ADMIN / MODERATION

| Method | Path | Mô tả | Role |
|--------|------|-------|------|
| GET  | `/admin/moderation-queue` | DS opp PENDING | Admin |
| POST | `/admin/opportunities/:id/approve` | Duyệt (PENDING→APPROVED, set published_at) | Admin |
| POST | `/admin/opportunities/:id/reject` | Từ chối (PENDING→REJECTED, body reason) | Admin |
| POST | `/admin/opportunities/:id/request-update` | Yêu cầu NTD cập nhật tin (→DRAFT + `ai_moderation_note` + thông báo) | Admin |
| POST | `/admin/opportunities/:id/feature` | Set `is_featured` + `featured_until` | Admin |
| POST | `/admin/categories` | Thêm (is_system=false) / sửa / xóa (chỉ is_system=false) | Admin |
| POST | `/admin/domains` | CRUD lĩnh vực | Admin |
| GET  | `/admin/analytics` | Tổng quan hệ thống | Admin |
| GET  | `/admin/users` | Quản lý user / verify provider | Admin |
| POST | `/admin/users/:id/verify-org` | Duyệt org thành VERIFIED (**400 nếu thiếu hồ sơ org**) | Admin |
| GET  | `/admin/orgs/:orgId/documents` | Xem hồ sơ xác minh tổ chức | Admin |
| GET  | `/admin/orgs/:orgId/tax-check` | **Lớp 1** — check MST (format+checksum), không AI | Admin |
| GET  | `/admin/users/:id/tax-check-org` | **Lớp 1** — check MST theo owner | Admin |
| POST | `/admin/orgs/:orgId/ai-scan` | **Lớp 1** — AI quét hồ sơ tổ chức (thuế/pháp nhân) | Admin |
| POST | `/admin/users/:id/ai-scan-org` | **Lớp 1** — AI quét org theo owner userId | Admin |
| POST | `/admin/opportunities/:id/ai-scan` | **Lớp 2** — AI quét hồ sơ tin đăng (chương trình/ủy quyền) | Admin |
| GET  | `/admin/opportunities/:id/documents` | Xem hồ sơ liên quan tin chờ duyệt | Admin |
| GET  | `/admin/applications` | DS đơn SUBMITTED/REVIEWING (hoặc theo `oppId`) | Admin |
| POST | `/admin/applications/:appId/ai-scan` | **Lớp 3** — AI quét 1 CV/hồ sơ SV | Admin |
| POST | `/admin/opportunities/:id/applications/ai-scan` | **Lớp 3** — AI quét hàng loạt + nhóm lý do | Admin |
| POST | `/admin/applications/:appId/request-update` | Gửi lý do yêu cầu SV cập nhật (`APP_UPDATE_REQUIRED`) | Admin |

> Cần `OPENROUTER_API_KEY` / model `google/gemini-3-flash-preview`. **Hai lớp tách biệt:**
>
> **Lớp 1 — Tổ chức / thuế** (`…/ai-scan-org`, `…/tax-check`): GPKD·MST·định danh.  
> `taxCheck` deterministic (10/13 số + checksum). `apply=true`: APPROVE + tax PASS + confidence≥0.75 → `VERIFIED`; còn lại → `NEEDS_UPDATE` + `ORG_UPDATE_REQUIRED`.  
> Response thêm: `taxCheck`, `forgeryRisk`, `aiGeneratedSuspected`, `authenticitySignals`, `consistencyIssues`.
>
> **Lớp 2 — Tin đăng** (`POST /admin/opportunities/:id/ai-scan`): `PROGRAM_PROOF` / `PARTNERSHIP_LETTER` — khớp nội dung tin, spam/lừa đảo. **Không check MST.**  
> `apply=true`: chỉ tự `REJECT` tin khi verdict=REJECT ≥0.75; **không tự APPROVE** (Admin bấm Duyệt). Org chưa `VERIFIED` → không gợi ý APPROVE tin.
| GET  | `/admin/audit-logs` | Đọc audit | Admin |

---

### H. NOTIFICATIONS (Mục 5)

| Method | Path | Mô tả | Role |
|--------|------|-------|------|
| GET  | `/me/notifications` | DS thông báo (IN_APP) | User |
| POST | `/me/notifications/:id/read` | Đánh dấu đọc | User |
| GET  | `/me/notification-preferences` | Xem preference | User |
| PUT  | `/me/notification-preferences` | Sửa (loại/kênh/tần suất/lĩnh vực) | User |
| POST | `/me/device-tokens` | Đăng ký push token | User |

Loại thêm: `ORG_UPDATE_REQUIRED` — khi AI/Admin yêu cầu provider cập nhật hồ sơ tổ chức.

---

## 4. STATE MACHINE ENFORCEMENT

### 4.1 Opportunity (Mục 3.1 + Danh_Gia 3.2)
```
DRAFT --submit--> PENDING --approve--> APPROVED <--> HIDDEN
                         PENDING --reject--> REJECTED
APPROVED --close--> CLOSED
APPROVED --(deadline passed, cron)--> EXPIRED
```
- Cron job mỗi giờ: opp `APPROVED` & `deadline < now` → `EXPIRED` (tự động).
- REJECTED/DRAFT/CLOSED/EXPIRED KHÔNG hiện trên board công khai.

### 4.2 Application (Mục 2 — chuẩn hóa 6 trạng thái)
```
SUBMITTED --(provider xem)--> REVIEWING
SUBMITTED --(từ chối ngay)--> REJECTED
REVIEWING --(mời PV)--> INTERVIEW
REVIEWING --(từ chối)--> REJECTED
INTERVIEW --(nhận)--> ACCEPTED
INTERVIEW --(từ chối)--> REJECTED
SUBMITTED/REVIEWING --(SV rút)--> WITHDRAWN
```
**Server guard (`PUT /provider/applications/:app_id/status`):**
- Chỉ chuyển tiến, KHÔNG lùi (INTERVIEW ≠ REVIEWING).
- Chỉ Provider owner của opp đó (lookup opp_id→org_id→org_members). Admin được force.
- Mỗi chuyển đổi ghi `application_status_history` + tạo `Notification(APP_STATUS)` cho student.
- Khi opp → CLOSED/EXPIRED/REJECTED: app đang SUBMITTED/REVIEWING auto → `WITHDRAWN` (S7), thông báo student.

---

## 5. NOTIFICATION WORKERS (Mục 5.4)

- **Producer NEW_OPP:** khi opp APPROVED → scan `notification_preferences` khớp category/domain → tạo `Notifications` (EMAIL/PUSH) theo frequency.
- **Scheduler DEADLINE_ALERT:** mỗi giờ quét `saved_opportunities` có opp `display_status=OPEN` & `deadline` trong `[now + notify_before_hours]` → 1 alert/user.
- **Consumer APP_STATUS / OPP_REJECTED / OPP_APPROVED:** event-driven từ change handler.
- Retry queue cho gửi fail; track `sent_at`.

---

## 6. BẢO MẬT & NFR (tích hợp phát hiện)

- **XSS (Mục 5):** `description`/`requirements`/`benefits` lưu dạng sanitized HTML (allowlist tag) hoặc Markdown → render an toàn. Backend sanitize trước lưu.
- **IDOR/BOLA:** mọi endpoint Provider/Admin join qua ownership; không tin param `org_id` từ client.
- **Rate-limit:** login 5/sai→lock; post opp quota; API 100 req/ip/phút.
- **Mã hóa:** CV lưu object storage, file-level encrypt (KMS); DB chỉ lưu URL. PII mask trong log.
- **Audit:** mọi approve/reject/feature/đổi role → `audit_logs` + `moderation_logs`.
- **PDPD 2023 (C1):** có `DELETE /me` (xóa hồ sơ + CV), retention CV 12 tháng, consent khi đăng ký.
- **Performance:** GIN index tìm kiếm, cursor pagination, cache board 60s (Redis), CDN banner.

---

## 7. ROADMAP TRIỂN KHAI

| Phase | Hạng mục |
|-------|----------|
| P0 | Auth + RBAC + schema.sql + F01/F02/F03 + F05.1 + F06.1 + apply 1 mode (EXTERNAL trước nếu cần ship nhanh) |
| P1 | Apply INTERNAL + status tracking + F05.3 export + email cơ bản + Featured + Related |
| P2 | Push + preference + alert deadline + analytics đầy đủ + org multi-user + paid featured |

---

*Tài liệu thiết kế API — bổ sung cho `schema.sql` và các báo cáo phân tích trong docs/. Tích hợp đầy đủ 5 lỗ hổng (Auth, State Machine, Featured, Fields, Notification).*
