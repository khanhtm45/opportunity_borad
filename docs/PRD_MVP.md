# PRD_MVP — OPPORTUNITY BOARD (Backend)
## Product Requirements Document — Phạm vi MVP

> Dựa trên: `Phan_Tich_Chuc_Nang_Opportunity_Board.md`, `Phan_Tich_Danh_Gia_Opportunity_Board.md`, `Phan_Tich_Chi_Tiet_Lo_Hong.md`, `docs/db/schema.sql`, `docs/api/API_SPEC.md`, `docs/db/ERD.md`, và source `backend/`.

---

## 1. MỤC TIÊU MVP

Xây dựng backend vận hành được của Bảng tin Cơ hội Sinh viên với:
- Đăng nhập/phân quyền 3 role an toàn (Auth hoàn chỉnh — lỗ hổng #1 đã đóng).
- Board công khai, tìm kiếm/lọc, chi tiết (F01–F03).
- Provider đăng tin + Admin kiểm duyệt (F05.1 + F06.1).
- Ứng tuyển nội bộ có state machine + chặn IDOR (lỗ hổng #2 đã đóng).
- Bookmark + Notification cơ bản (lỗ hổng #5 đã đóng).
- Featured (lỗ hổng #3) + counters (lỗ hổng #4) đã có sẵn trong schema.

**Không nằm trong MVP:** push thực tế (chỉ lưu record), export CSV thực tế (chỉ endpoint placeholder), paid featured, org multi-user đầy đủ, analytics chart, social share tracking.

---

## 2. PHẠM VI THEO PHASE (từ API_SPEC §7)

### P0 — "Board chạy được + có tin thật" (ĐÃ IMPLEMENT TRONG BACKEND NÀY)
| Chức năng | File / Endpoint | Trạng thái |
|-----------|----------------|-----------|
| Auth Student/Provider/Admin | `AuthController`, `AuthService` | ✅ |
| Phân quyền RBAC + JWT + lockout | `SecurityConfig`, `JwtTokenProvider`, `CurrentUser` | ✅ |
| F01 Board list + Featured | `OpportunityController`, `OpportunityService.listPublic/Featured` | ✅ |
| F02 Search multi-filter (GIN tsvector) | `OpportunityRepository.search` | ✅ |
| F03 Detail + related | `OpportunityService.detail` | ✅ |
| F05.1 tạo + submit duyệt | `OpportunityService.create/submit` | ✅ |
| F06.1 approve/reject + log | `OpportunityService.approve/reject` + `ModerationLog` | ✅ |
| F04.2 Bookmark (chưa alert) | `SavedOpportunityService` | ✅ |
| Schema DB + Flyway | `V1__init_schema.sql` | ✅ |

### P1 — Engagement & Provider ops
- Apply INTERNAL đầy đủ + status tracking (đã có `ApplicationService` + state machine) → **chỉ cần viết controller Student apply (đã có) + Provider đổi status (đã có)**.
- F05.3 quản lý CV + export CSV thực tế.
- Email thông báo thực tế (`NotificationServiceImpl` đang lưu DB → bổ sung `JavaMailSender`).
- Featured banner (đã có field, cần admin set).
- Related opportunities (đã có).

### P2 — Growth & vận hành
- Push notification (FCM) + preference theo category/domain.
- Analytics dashboard (counters đã có).
- Alert deadline bookmark (Scheduler đã có `deadlineAlerts`, cần gửi email).
- Share social deep-link tracking.
- Org multi-user (`OrgMember` đã có), paid featured.

---

## 3. USER STORIES (P0)

1. **US-01** — *Student* đăng ký bằng email/mật khẩu, xác thực email, đăng nhập nhận JWT.
2. **US-02** — *Student* mở board, xem card, lọc theo category/location/status, sort.
3. **US-03** — *Student* mở detail, xem mô tả/requirements, xem related.
4. **US-04** — *Student* bookmark cơ hội để xem sau.
5. **US-05** — *Provider* đăng ký org (PENDING verify), đợi Admin duyệt verify.
6. **US-06** — *Provider* (đã verified) tạo tin (DRAFT) → submit → chờ duyệt.
7. **US-07** — *Admin* vào queue, duyệt (APPROVED, len board) hoặc từ chối (gửi lý do).
8. **US-08** — *Student* nhận notification khi có opp mới / opp bookmark sắp hết hạn (P1 gửi thực).

---

## 4. ACCEPTANCE CRITERIA (tiêu biểu)

| AC | Tiêu chí | Kiểm chứng |
|----|----------|------------|
| AC-01 | Đăng ký Admin bị từ chối | `AuthService.register` ném `UnauthorizedException` khi role=ADMIN |
| AC-02 | Login sai 5 lần → khóa 15p | `failedLoginCount>=5` → `lockedUntil` |
| AC-03 | Provider chưa verify không đăng tin | `create()` ném `ForbiddenException` nếu `verifiedStatus!=VERIFIED` |
| AC-04 | Apply EXTERNAL bị chặn nộp CV | `applyInternal` ném `ConflictException` |
| AC-05 | Không nộp trùng | UNIQUE(opp_id, student_id) → `ConflictException` |
| AC-06 | Provider A không sửa tin Provider B | `requireOwner` ném `ForbiddenException` (chống IDOR) |
| AC-07 | App không lùi trạng thái | `isForward` trả false → `ConflictException` |
| AC-08 | Opp quá hạn tự EXPIRED | `BoardScheduler.expireOverdue` chạy mỗi giờ |
| AC-09 | Featured tự rớt khi hết `featuredUntil` | query `featuredUntil > now` |
| AC-10 | Token vô hiệu sau đổi mk | `passwordVersion` mismatch → reject |

---

## 5. SCHEMA & MÔ HÌNH DỮ LIỆU (đã validate)

- 16 bảng + 1 VIEW (`opportunity_display_status`), 13 ENUM.
- `schema.sql` chạy thực tế trên PostgreSQL 18 → OK (17 objects).
- Flyway `V1__init_schema.sql` trong `backend/src/main/resources/db/migration`.

---

## 6. BẢO MẬT & TUÂN THỦ (đã tích hợp)

- JWT access 15p + refresh 14 ngày, revoke qua `passwordVersion`.
- BCrypt hash, lockout brute-force.
- RBAC server-side (`@PreAuthorize` + `requireOwner` trong service).
- XSS: `description`/`requirements`/`benefits` lưu sanitized (cần thêm sanitizer thực tế ở P1).
- PDPD 2023: `DELETE /me` (chưa có, P1), retention CV 12 tháng (chưa có job, P1), consent (P1).

---

## 7. NON-FUNCTIONAL (mục tiêu)

- Response search/filter < 2s: GIN `to_tsvector` + cursor pagination + index.
- 5000+ concurrent: cache board (Redis, P1), CDN banner (P1).
- Validation tập trung: DTO `@Valid`, `GlobalExceptionHandler` chuẩn hóa lỗi.

---

## 8. RỦI RO & GIẢM THIỂU

| Rủi ro | Mức | Giảm thiểu |
|--------|-----|-----------|
| Cold start (board trống) | Cao | Seed opp mẫu qua Flyway/V2 |
| Spam tin giả | Cao | verify Provider bắt buộc + moderation |
| External apply mất attribution | Trung bình | `external-click` log (đã có endpoint placeholder) |
| Thiếu sanitizer XSS | Cao | Bổ sung OWASP Java HTML Sanitizer ở P1 |

---

## 9. ĐỊNH NGHĨA DONE (MVP P0)

- [x] Backend compile + package thành công (`mvn package` → jar 54MB).
- [x] Schema chạy được trên PostgreSQL thực.
- [x] Auth + RBAC + 3 role.
- [x] F01/F02/F03 công khai hoạt động.
- [x] F05.1 + F06.1 (duyệt/từ chối + log).
- [x] Bookmark.
- [ ] Email/Push thực tế (P1).
- [ ] Sanitizer XSS (P1).
- [ ] Test tích hợp (P1).

---

## 10. CÁC BƯỚC TIẾP THEO

1. Viết test tích hợp (Spring Boot Test + Testcontainers Postgres) cho state machine + IDOR guard.
2. Bổ sung `JavaMailSender` trong `NotificationServiceImpl`.
3. Thêm OWASP sanitizer cho rich text.
4. Wire frontend (React/Vue) vào API đã có.
5. Deploy: Dockerfile + GitHub Actions CI.

---

*PRD_MVP — tích hợp toàn bộ phát hiện phân tích. Backend Spring Boot đã build thành công.*
