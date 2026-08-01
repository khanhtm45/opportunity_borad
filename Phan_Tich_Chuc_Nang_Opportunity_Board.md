# TÀI LIỆU PHÂN TÍCH CHỨC NĂNG HỆ THỐNG
## Phân Hệ: Opportunity Board (Bảng Tin Cơ Hội Sinh Viên)

---

## 1. TỔNG QUAN TÍNH NĂNG (OVERVIEW)

### 1.1 Tên phân hệ / Chức năng
* **Tên tiếng Anh:** Opportunity Board
* **Tên tiếng Việt:** Bảng tin cơ hội điện tử dành cho Sinh viên

### 1.2 Bối cảnh & Mô tả tổng quan
Trong môi trường đại học và hệ sinh thái khởi nghiệp, sinh viên thường gặp khó khăn trong việc tiếp cận các thông tin tuyển dụng, học bổng, các cuộc thi hay quỹ đầu tư do thông tin bị phân tán trên nhiều kênh rời rạc (Facebook, Email, Website trường, Diễn đàn). 

**Opportunity Board** là bảng tin điện tử tập trung toàn bộ các cơ hội phát triển dành cho sinh viên. Hệ thống đóng vai trò cầu nối thông tin giữa Sinh viên với Doanh nghiệp, Startup, Quỹ đầu tư và các Ban tổ chức cuộc thi.

### 1.3 Mục tiêu & Giá trị mang lại
* **Đối với Sinh viên:** Tiếp cận nhanh chóng, đầy đủ và chính xác các cơ hội phát triển nghề nghiệp, nâng cao kỹ năng, tìm kiếm học bổng và dự án khởi nghiệp.
* **Đối với Nhà tuyển dụng / Đơn vị tổ chức:** Tiếp cận đúng đối tượng mục tiêu là sinh viên năng động, tối ưu hóa quy trình truyền thông và thu hút hồ sơ/đăng ký.
* **Đối với Nhà trường / Hệ thống:** Quản lý tập trung các hoạt động kết nối doanh nghiệp, theo dõi sự tham gia và phát triển của sinh viên.

---

## 2. BẢNG TÁC NHÂN VÀ PHÂN QUYỀN (ACTORS & ROLES)

| Tác nhân (Actor) | Vai trò & Trách nhiệm chính |
| :--- | :--- |
| **Sinh viên (Student)** | Xem, tìm kiếm, lọc các cơ hội; lưu cơ hội quan tâm; nộp hồ sơ / ứng tuyển trực tiếp; nhận thông báo về các cơ hội mới. |
| **Đơn vị đăng tin (Opportunity Provider)**<br>*(Doanh nghiệp, Startup, BTC cuộc thi, Quỹ đầu tư, CLB)* | Đăng tải tin tuyển dụng, chương trình, học bổng; quản lý danh sách sinh viên ứng tuyển/đăng ký; cập nhật trạng thái bài đăng. |
| **Quản trị viên (Admin / Moderator)** | Kiểm duyệt nội dung tin đăng; quản lý danh mục cơ hội; thống kê báo cáo hiệu quả; quản lý tài khoản người dùng và nhà tuyển dụng. |

---

## 3. CẤU TRÚC PHÂN TRÃI CHỨC NĂNG (FUNCTIONAL BREAKDOWN)

```
Opportunity Board
├── F01. Quản lý & Hiển thị Bảng tin (Opportunity Board View)
├── F02. Tìm kiếm & Bộ lọc nâng cao (Search & Filter)
├── F03. Xem chi tiết cơ hội (Opportunity Detail)
├── F04. Tương tác Sinh viên (Student Engagement)
│   ├── F04.1 Nộp hồ sơ / Ứng tuyển (Apply)
│   ├── F04.2 Lưu cơ hội yêu thích (Bookmark)
│   └── F04.3 Chia sẻ tin (Share)
├── F05. Quản lý Đăng tin (Opportunity Management - Provider)
│   ├── F05.1 Tạo & Chỉnh sửa bài đăng
│   └── F05.2 Quản lý hồ sơ ứng tuyển
└── F06. Kiểm duyệt & Quản trị (Admin Moderation & Analytics)
```

---

## 4. MA TRẬN CHỨC NĂNG CHI TIẾT (DETAILED FUNCTIONAL REQUIREMENTS)

### 4.1 Danh mục Loại hình Cơ hội (Opportunity Categories)
Hệ thống phân loại các cơ hội thành **7 nhóm chính**:
1. **Thực tập (Internship):** Các vị trí thực tập doanh nghiệp, thực tập ngắn hạn/dài hạn.
2. **Tuyển dụng Startup (Startup Recruitment):** Tuyển dụng nhân sự, Co-founder, CTV cho các dự án khởi nghiệp.
3. **Cuộc thi Đổi mới Sáng tạo (Innovation Contests):** Các giải đấu, cuộc thi ý tưởng kinh doanh, sáng tạo công nghệ.
4. **Hackathon:** Các cuộc thi lập trình, phát triển sản phẩm nhanh.
5. **Học bổng (Scholarships):** Học bổng khuyến học, học bổng doanh nghiệp, học bổng du học / trao đổi.
6. **Quỹ đầu tư (Investment Funds):** Các đợt rót vốn, tài trợ dự án khởi nghiệp sinh viên.
7. **Chương trình ươm tạo (Incubators / Accelerators):** Các khóa huấn luyện, ươm tạo dự án khởi nghiệp.

---

### 4.2 Chi tiết từng Chức năng

#### F01: Quản lý & Hiển thị Bảng tin (Board Display & Listing)
* **F01.1 Dashboard Bảng tin:**
  * Hiển thị danh sách các bài đăng dưới dạng Card (Thẻ thông tin) hoặc List view.
  * Mỗi Thẻ bài đăng bao gồm: Banner/Logo, Tiêu đề, Đơn vị tổ chức, Tag loại hình cơ hội, Hạn chót (Deadline), Địa điểm/Hình thức (Online/Offline), Trạng thái (Đang mở / Sắp kết thúc).
* **F01.2 Cơ hội Nổi bật (Featured Opportunities):**
  * Khu vực Banner Top / Slider dành cho các cơ hội HOT, tài trợ hoặc ưu tiên đẩy tin.
* **F01.3 Phân trang / Cuộn vô tận (Pagination / Infinite Scroll):**
  * Tải dữ liệu mượt mà, tối ưu hiệu năng hiển thị.

#### F02: Tìm kiếm & Bộ lọc Nâng cao (Search & Filtering System)
* **F02.1 Tìm kiếm theo từ khóa (Keyword Search):**
  * Tìm theo tên bài đăng, tên doanh nghiệp, kỹ năng/yêu cầu.
* **F02.2 Bộ lọc Đa tiêu chí (Multi-criteria Filter):**
  * **Lọc theo loại hình:** Chọn 1 hoặc nhiều trong 7 nhóm cơ hội.
  * **Lọc theo hình thức:** Online, Offline, Hybrid.
  * **Lọc theo vị trí / Địa điểm:** Hà Nội, TP.HCM, Đà Nẵng, Toàn quốc, Quốc tế...
  * **Lọc theo trạng thái:** Còn hạn nộp, Sắp hết hạn (trong 3 ngày), Đã đóng.
  * **Lọc theo lĩnh vực:** IT, Marketing, Tài chính, Thiết kế, Khởi nghiệp...
* **F02.3 Sắp xếp (Sorting):**
  * Theo Ngày đăng mới nhất, Hạn nộp gần nhất, Mức độ phổ biến (lượt xem/lượt ứng tuyển).

#### F03: Xem chi tiết Cơ hội (Opportunity Detail Page)
* **Thông tin chung:** Tiêu đề, Đơn vị phát động, Thời gian đăng, Hạn chót ứng tuyển.
* **Mô tả chi tiết:**
  * Nội dung chương trình / Mô tả công việc.
  * Yêu cầu đối với ứng viên / sinh viên.
  * Quyền lợi & Giải thưởng / Mức lương (nếu có).
  * Quy trình tuyển chọn / Lịch trình cuộc thi.
* **Thông tin đơn vị đăng tải:** Giới thiệu ngắn về doanh nghiệp/BTC, thông tin liên hệ, website.
* **Các cơ hội liên quan (Related Opportunities):** Gợi ý các tin đăng cùng lĩnh vực.

#### F04: Tương tác của Sinh viên (Student Features)
* **F04.1 Ứng tuyển / Đăng ký (Application Flow):**
  * Nộp hồ sơ trực tuyến qua form của hệ thống (Đính kèm CV/Profile, Thư giới thiệu).
  * Hoặc điều hướng trực tiếp đến Link đăng ký bên ngoài (External Link) do BTC/Doanh nghiệp cung cấp.
  * Theo dõi trạng thái ứng tuyển (Đã nộp, Đang xem xét, Trúng tuyển/Mời phỏng vấn, Từ chối).
* **F04.2 Lưu cơ hội (Save / Bookmark):**
  * Sinh viên bấm lưu bài đăng để xem lại sau trong mục "Cơ hội đã lưu".
  * Nhận cảnh báo (Alert) trước khi bài đăng hết hạn 24h-48h.
* **F04.3 Chia sẻ & Thông báo:**
  * Chia sẻ bài đăng qua mạng xã hội (Facebook, LinkedIn) hoặc sao chép liên kết.
  * Đăng ký nhận thông báo email/push notification khi có cơ hội mới thuộc lĩnh vực quan tâm.

#### F05: Quản lý Tin đăng - Dành cho Đơn vị Đăng tin (Provider Portal)
* **F05.1 Tạo bài đăng mới:**
  * Form nhập liệu chuẩn hóa (Tiêu đề, Loại cơ hội, Banner, Nội dung rich text, Yêu cầu, Hạn nộp, Link ứng tuyển/Form nộp CV).
  * Xem trước bài đăng (Preview) trước khi gửi duyệt.
* **F05.2 Quản lý bài đăng:**
  * Danh sách bài đăng: Cho phép Ẩn/Hiện, Đóng sớm, Gia hạn deadline, Chỉnh sửa thông tin.
* **F05.3 Quản lý Hồ sơ ứng tuyển (CV Management):**
  * Danh sách sinh viên đã nộp CV/Đăng ký.
  * Tải xuống danh sách (Xuất file Excel/CSV) hoặc duyệt CV trực tiếp trên hệ thống.
  * Thay đổi trạng thái hồ sơ và gửi email phản hồi tự động cho sinh viên.

#### F06: Kiểm duyệt & Quản trị Hệ thống (Admin & Moderation)
* **F06.1 Bảng kiểm duyệt tin (Content Moderation Queue):**
  * Admin duyệt hoặc từ chối các tin đăng từ doanh nghiệp/đối tác trước khi hiển thị lên Bảng tin công khai.
  * Gửi phản hồi/lý do từ chối về tài khoản đăng tin.
* **F06.2 Quản lý Danh mục & Tag:**
  * Thêm/Sửa/Xóa các danh mục cơ hội, lĩnh vực chuyên môn, tag sự kiện.
* **F06.3 Thống kê & Báo cáo (Analytics Dashboard):**
  * Tổng số cơ hội đang hoạt động.
  * Thống kê lượt tương tác: Lượt xem (Views), Lượt lưu (Bookmarks), Lượt ứng tuyển (Applications).
  * Biểu đồ tỷ lệ phân bổ cơ hội theo loại hình (Thực tập vs Cuộc thi vs Học bổng...).

---

## 5. QUY TRÌNH NGHIỆP VỤ CHÍNH (BUSINESS WORKFLOWS)

### 5.1 Quy trình Đăng tin & Kiểm duyệt (Posting & Approval Workflow)
1. **Đơn vị cung cấp cơ hội** đăng nhập -> Chọn "Tạo cơ hội mới".
2. Nhập đầy đủ thông tin vào Form -> Chọn "Gửi kiểm duyệt".
3. **Quản trị viên (Admin)** nhận thông báo -> Kiểm tra nội dung tin đăng (tính chính xác, văn hóa, không lừa đảo).
4. **Kết quả:**
   * *Nếu Duyệt:* Bài đăng tự động xuất hiện trên **Opportunity Board**.
   * *Nếu Từ chối:* Hệ thống gửi email kèm lý do yêu cầu điều chỉnh.

### 5.2 Quy trình Sinh viên Tiếp cận & Ứng tuyển (Student Journey)
1. **Sinh viên** truy cập **Opportunity Board**.
2. Tìm kiếm / Sử dụng bộ lọc theo nhu cầu (ví dụ: Lọc "Hackathon" hoặc "Thực tập").
3. Mở bài đăng chi tiết để nghiên cứu yêu cầu và quyền lợi.
4. Thực hiện hành động:
   * **Lưu tin:** Để theo dõi sau.
   * **Ứng tuyển trực tiếp:** Nộp CV lên hệ thống -> Hệ thống chuyển CV đến Đơn vị đăng tin.
   * **Đăng ký ngoài:** Click chuyển hướng sang Landing page/Form của Ban tổ chức.

---

## 6. YÊU CẦU PHI CHỨC NĂNG (NON-FUNCTIONAL REQUIREMENTS)

| Tiêu chí | Yêu cầu chi tiết |
| :--- | :--- |
| **Giao diện & Trải nghiệm (UI/UX)** | * Thiết kế Responsive, tối ưu hóa trên cả Desktop và Mobile.<br>* Giao diện hiện đại, trực quan, phân màu rõ ràng cho 7 loại hình cơ hội.<br>* Tải trang nhanh, trải nghiệm tìm kiếm và lọc không bị giật lag. |
| **Hiệu năng (Performance)** | * Thời gian phản hồi của hệ thống < 2 giây cho các tác vụ tìm kiếm/lọc.<br>* Hỗ trợ đồng thời 5,000+ sinh viên truy cập cùng lúc trong các đợt cao điểm. |
| **Bảo mật & Quyền riêng tư (Security)** | * Mã hóa thông tin cá nhân và CV của sinh viên.<br>* Phân quyền chặt chẽ: Doanh nghiệp chỉ xem được hồ sơ ứng tuyển vào tin đăng của chính mình.<br>* Chống Spam/Tin lừa đảo bằng quy trình duyệt nghiêm ngặt. |
| **Khả năng mở rộng (Scalability)** | * Thiết kế API phẳng, dễ dàng tích hợp với Hệ thống quản lý sinh viên (LMS/CRM), Hệ thống gửi Email/SMS tự động, hoặc Cổng thanh toán (nếu mở rộng tính năng đăng tin trả phí). |

---

## 7. MÔ HÌNH DỮ LIỆU SƠ BỘ (PROPOSED DATA ENTITIES)

1. **`Users`**: `user_id`, `full_name`, `email`, `role` (Student, Provider, Admin), `created_at`.
2. **`StudentProfiles`**: `profile_id`, `user_id`, `major`, `university_year`, `cv_url`, `skills`, `bio`.
3. **`Organizations`**: `org_id`, `user_id`, `org_name`, `logo`, `website`, `description`, `verified_status`.
4. **`Opportunities`**: `opp_id`, `org_id`, `title`, `category_id` (Thực tập, Hackathon, ...), `description`, `requirements`, `benefits`, `location`, `work_type`, `deadline`, `status` (Draft, Pending, Approved, Rejected, Closed), `external_link`, `created_at`.
5. **`Categories`**: `category_id`, `category_name`, `code` (INTERNSHIP, HACKATHON, SCHOLARSHIP...).
6. **`Applications`**: `app_id`, `opp_id`, `student_id`, `cv_file`, `cover_letter`, `status` (Submitted, Reviewed, Accepted, Rejected), `applied_at`.
7. **`SavedOpportunities`**: `id`, `student_id`, `opp_id`, `saved_at`.

---
*Tài liệu phân tích chức năng được tổng hợp và xây dựng dựa trên yêu cầu hệ thống Opportunity Board.*
