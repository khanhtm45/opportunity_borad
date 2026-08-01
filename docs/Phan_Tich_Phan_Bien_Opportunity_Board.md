# BÁO CÁO PHÂN TÍCH PHẢN BIỆN (CRITICAL REVIEW)
## Phân Hệ: Opportunity Board (Bảng Tin Cơ Hội Sinh Viên)

> Tài liệu nguồn: `Phan_Tich_Chuc_Nang_Opportunity_Board.md`
> Mục tiêu: Rà soát các khoảng trống, mâu thuẫn, rủi ro và điểm cần bổ sung trước khi bước vào thiết kế/kỹ thuật.

---

## 1. ĐÁNH GIÁ TỔNG QUAN — ĐIỂM TỐT

- Phân định tác nhân (Actor) rõ ràng: Student / Provider / Admin — đúng 3 nhóm quyền, dễ ánh xạ vào RBAC.
- Cấu trúc phân rã chức năng (mục 3 tài liệu gốc) có thứ bậc hợp lý, bao quát luồng Student, Provider, Admin.
- 7 nhóm loại hình cơ hội (mục 4.1) định nghĩa cụ thể, dễ chuẩn hóa thành bảng Categories.
- Quy trình nghiệp vụ (mục 5) mô tả đúng thực tế: duyệt trước khi public, sinh viên có 2 đường ứng tuyển (trong/ngoài).
- Đã có mục Phi chức năng (mục 6) — hiếm trong tài liệu mức này, đáng ghi nhận.

**Kết luận sơ bộ:** Tài liệu ở mức "phác thảo ý định" chưa đạt mức "yêu cầu có thể đưa vào thiết kế". Các lỗ hổng dưới đây cần đóng trước khi viết code.

---

## 2. KHOẢNG TRỐNG CHỨC NĂNG LỚN (CRITICAL GAPS)

### 2.1. Không có phân hệ Xác thực & Quản lý tài khoản (Auth/Account)
- Mục 2 định nghĩa 3 role nhưng KHÔNG hề mô tả: đăng ký, đăng nhập, quên mật khẩu, xác thực email, khóa/mở tài khoản, OAuth (Google/ trường).
- Mọi workflow (5.1, 5.2) đều bắt đầu bằng "đăng nhập" nhưng cơ chế đăng nhập không tồn tại trong tài liệu.
- Chưa rõ: Provider đăng ký như thế nào? Ai cấp quyền Provider (tự đăng ký hay Admin tạo)? Cơ chế xác minh doanh nghiệp (`Organizations.verified_status`) không có quy trình.

### 2.2. Thiếu phân hệ Thông báo (Notification Subsystem)
- F04.3 nhắc đến email/push notification và cảnh báo hết hạn 24-48h (F04.2), đăng ký nhận thông báo theo lĩnh vực — nhưng KHÔNG có chức năng nào quản lý việc này (không có F07).
- Thiếu: mẫu thông báo, tần suất gửi, cơ chế unsubscribe, quản lý preference (theo loại hình/lĩnh vực nào) của sinh viên.

### 2.3. Thiếu quy trình vòng đời hồ sơ ứng tuyển (Application lifecycle)
- F04.1 liệt kê trạng thái: Đã nộp / Đang xem xét / Trúng tuyển-Mời phỏng vấn / Từ chối.
- Nhưng mục 7 (Applications.status) chỉ có: Submitted, Reviewed, Accepted, Rejected — **THIẾU** trạng thái "Interview/Invited" (Mời phỏng vấn).
- Mô hình dữ liệu và mô tả chức năng **BẤT ĐỒNG BỘ** (xem mục 4).

### 2.4. Thiếu xử lý khi Opportunity bị đóng/từ chối
- Khi opp chuyển sang Closed hoặc Rejected, các Applications/SavedOpportunities phụ thuộc sẽ ra sao? Tự động thu hồi? Giữ lịch sử? Chưa định nghĩa.
- Sinh viên đã lưu một opp bị từ chối thì "Cơ hội đã lưu" hiển thị thế nào?

### 2.5. Thiếu quy tắc "Ứng tuyển trong vs ngoài"
- F04.1 có 2 đường (nộp CV trên hệ thống HOẶC link ngoài) nhưng không có luật: opp nào dùng đường nào? Trường `external_link` có thể null? Nếu có `external_link` thì có disable form nộp CV không? Cần ràng buộc rõ.

---

## 3. MÂU THUẪN & MƠ HỒ (AMBIGUITY / CONTRADICTION)

### 3.1. Trạng thái Opportunity
- Mô hình (mục 7): `status = Draft, Pending, Approved, Rejected, Closed`.
- F01.1 hiển thị: "Đang mở / Sắp kết thúc" — đây là trạng thái **DẪN XUẤT** (từ deadline + Approved), không phải giá trị lưu. Tài liệu gộp chung gây nhầm lẫn giữa trạng thái lưu và trạng thái hiển thị. Cần tách biệt.

### 3.2. Categories: cố định hay cấu hình được?
- Mục 4.1 ghi "7 nhóm chính" (nghe như cố định).
- F06.2 lại ghi Admin "Thêm/Sửa/Xóa các danh mục cơ hội" (nghe như tự do cấu hình).
- Mâu thuẫn: nếu Admin xóa 1 trong 7 nhóm thì dữ liệu cũ xử lý sao? Cần quyết định: danh mục hệ thống (fixed) vs do Admin mở rộng (extensible).

### 3.3. Tags / Lĩnh vực
- F02.2 lọc "theo lĩnh vực: IT, Marketing..." và F06.2 "quản lý tag" — nhưng mô hình dữ liệu (mục 7) **KHÔNG có bảng Tags/Lĩnh vực** nào cả. `Opportunities` có `location`, `work_type`, `category_id` nhưng không có field lĩnh vực (field/domain). Thiếu entity.

### 3.4. Bookmark alert 24-48h
- F04.2 ghi "Nhận cảnh báo trước khi hết hạn 24h-48h" — khoảng 24-48h là một khoảng, sinh viên chọn hay hệ thống cố định? Chưa rõ.

---

## 4. MÔ HÌNH DỮ LIỆU — CÁC LỖI CỤ THỂ

- **Users:** thiếu `password_hash`, `status` (active/locked), `last_login`, `auth_provider`. Không thể hiện thực đăng nhập.
- **Organizations.verified_status:** thiếu quy trình cấp/trở thành verified (liên quan 2.1).
- **StudentProfiles.skills:** kiểu dữ liệu gì? (string JSON / bảng quan hệ N-N?). Chưa định nghĩa.
- **Applications:** THIẾU trạng thái Interview (đã nêu 2.3); thiếu trường ghi chú phản hồi của Provider; thiếu `updated_at`.
- **Opportunities:** thiếu trường quy định opp dùng form nộp hay external_link (`is_external` boolean); thiếu `view_count`, `apply_count` (F06.3 cần đếm lượt xem/ứng tuyển — nếu không lưu counter thì phải tính aggregate mỗi lần, tốn hiệu năng).
- **Thiếu bảng:** Tags/Domains, Notifications, AuditLog, (có thể) SavedSearch.
- **Quan hệ sở hữu:** `Applications.opp_id -> Opportunities.org_id -> Organizations.user_id`. Để enforce "Provider chỉ xem hồ sơ opp của mình" (F06 bảo mật) cần join chuỗi này — nên có index và constraint rõ ràng, tài liệu chưa chỉ ra.

---

## 5. BẢO MẬT & PHÁP LÝ — ĐIỂM YẾU

- Mục 6 ghi "mã hóa thông tin cá nhân và CV" nhưng không nói rõ: mã hóa tại rest hay chỉ transit (HTTPS)? Khoá do ai quản lý? CV là dữ liệu nhạy cảm, nên có mã hóa tệp (file-level) + quyền truy cập hạt nhân.
- **Không có quy định Vòng đời dữ liệu / Xoá dữ liệu (data retention & right-to-erasure).** Sinh viên có quyền yêu cầu xoá CV/hồ sơ không? — Quan trọng vì Việt Nam có **Nghị định 13/2023 (PDPD)** về bảo vệ dữ liệu cá nhân. Tài liệu hoàn toàn bỏ qua tuân thủ pháp lý.
- **Rich text** (F05.1 nội dung rich text, F03 mô tả) — nguy cơ **XSS** cao nếu render HTML từ Provider. Cần quy định: sanitize, cho phép tag whitelist, hoặc lưu Markdown thay vì HTML thô.
- **IDOR/BOLA:** F06 bảo mật nói đúng ý (Provider chỉ xem opp của mình) nhưng thực thi phụ thuộc vào kiểm tra quyền ở tầng API — cần quy định rõ là requirement bắt buộc, không để FE tự ẩn.
- Chống spam đăng tin: mục 6 nhắc "chống spam" nhưng không có cơ chế rate-limit, quota đăng tin, hay phạt tài khoản lặp lại bị reject.
- Không có **Audit Log** cho Admin (ai duyệt/từ chối opp nào, lúc nào) — cần cho trách nhiệm giải trình kiểm duyệt.

---

## 6. YÊU CẦU PHI CHỨC NĂNG — THIẾU SÓT

- **UI/UX:** OK cơ bản, nhưng thiếu quy định Accessibility (WCAG), và i18n (chỉ tiếng Việt hay đa ngôn ngữ?).
- **Performance:** <2s, 5000+ concurrent — thiếu chỉ số cụ thể hơn: latency P95, throughput, và cách đạt được (caching, full-text search index cho F02, CDN cho banner). F02 tìm kiếm đa tiêu chí trên 5000+ user cần chỉ định dùng giải pháp tìm kiếm (Elasticsearch/Postgres FTS) chứ không phải `LIKE`.
- **Scalability:** nhắc tích hợp LMS/CRM/Email/SMS/Payment — nhưng chưa có danh sách API/event cụ thể, thiếu bản thiết kế integration (webhook? queue?).
- **Thiếu:** yêu cầu uptime/SLA, backup/DR, logging/monitoring, bản đồ lỗi (error codes), validation rules (deadline phải ở tương lai, độ dài tiêu đề, dung lượng banner/CV tối đa, định dạng file CV cho phép).

---

## 7. ĐỀ XUẤT ƯU TIÊN (PRIORITY ROADMAP)

### MỨC A — Phải có trước khi thiết kế (blocker)
- **A1.** Bổ sung phân hệ Auth & Account (đăng ký/đăng nhập/phân quyền/verify Provider).
- **A2.** Chuẩn hóa trạng thái: tách "trạng thái lưu" (Draft/Pending/Approved/Rejected/Closed) khỏi "trạng thái hiển thị" (Đang mở/Sắp kết thúc/Đã đóng) — đồng bộ F01.1 với mô hình dữ liệu.
- **A3.** Sửa `Applications.status` thêm Interview; thêm phản hồi + `updated_at`.
- **A4.** Quyết định Categories fixed vs extensible; thêm bảng Tags/Domains.
- **A5.** Thêm bảng Notifications + quy trình thông báo (F04.3, F04.2 alert).

### MỨC B — Trước khi code (important)
- **B1.** Quy tắc opp "nộp trong / link ngoài" (`is_external` + ràng buộc).
- **B2.** Lifecycle dependent records (opp Closed/Rejected -> Applications/Saved xử lý sao).
- **B3.** Quy định XSS sanitize cho rich text; upload constraints (banner/CV size, format).
- **B4.** Audit Log cho Admin; rate-limit chống spam đăng tin.
- **B5.** Bổ sung counter `view_count`/`apply_count` vào `Opportunities` (cho F06.3).

### MỨC C — Nâng cao / tuân thủ
- **C1.** Tuân thủ PDPD 2023: retention, quyền xoá dữ liệu, consent.
- **C2.** Accessible (WCAG), i18n.
- **C3.** Bản thiết kế tích hợp LMS/CRM/Email/SMS (webhook/queue).
- **C4.** SLA/uptime/backup/monitoring, error code map, validation rules tập trung.

---

## 8. KẾT LUẬN

Tài liệu là bản phác thảo chức năng tốt về mặt bao quát nghiệp vụ, nhưng ở mức ý định. Trước khi chuyển sang thiết kế kỹ thuật (ER diagram, API spec, wireframe), cần đóng 5 blocker mức A — đặc biệt là:

1. **Phân hệ Auth hoàn toàn vắng mặt**, và
2. **Sự bất đồng bộ giữa mô tả trạng thái ở F01.1/F04.1 với mô hình dữ liệu ở mục 7.**

Hai điểm này nếu không sửa sớm sẽ dẫn đến phải thiết kế lại tầng dữ liệu và bảo mật.

---

## PHỤ LỤC — CÁC SẢN PHẨM CÓ THỂ TẠO TIẾP THEO

- Sơ đồ ER (Entity Relationship) đã sửa lỗi và bổ sung entity thiếu.
- Bảng yêu cầu chức năng dạng user-story + acceptance criteria (chuẩn làm việc với dev).
- Danh sách API endpoints dự kiến (REST) cho từng F01–F06.
- Ma trận phân quyền (role x chức năng) chi tiết.
