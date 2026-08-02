-- ============================================================================
-- DEMO SEED — Opportunity Board (Flyway V3)
-- Chạy SAU V1 + V2. Password tất cả user demo: password123
-- Idempotent: xóa data *@demo.ob.local rồi insert lại.
-- ============================================================================

-- 1) Dọn data demo cũ (theo email domain)
DELETE FROM application_status_history
 WHERE app_id IN (
   SELECT a.app_id FROM applications a
   JOIN users u ON u.user_id = a.student_id
   WHERE u.email LIKE '%@demo.ob.local'
 );
DELETE FROM applications
 WHERE student_id IN (SELECT user_id FROM users WHERE email LIKE '%@demo.ob.local')
    OR opp_id IN (
      SELECT o.opp_id FROM opportunities o
      JOIN users u ON u.user_id = o.created_by
      WHERE u.email LIKE '%@demo.ob.local'
    );
DELETE FROM saved_opportunities
 WHERE student_id IN (SELECT user_id FROM users WHERE email LIKE '%@demo.ob.local')
    OR opp_id IN (
      SELECT o.opp_id FROM opportunities o
      JOIN users u ON u.user_id = o.created_by
      WHERE u.email LIKE '%@demo.ob.local'
    );
DELETE FROM notifications WHERE user_id IN (SELECT user_id FROM users WHERE email LIKE '%@demo.ob.local');
DELETE FROM notification_preferences WHERE user_id IN (SELECT user_id FROM users WHERE email LIKE '%@demo.ob.local');
DELETE FROM moderation_logs
 WHERE opp_id IN (
   SELECT o.opp_id FROM opportunities o
   JOIN users u ON u.user_id = o.created_by
   WHERE u.email LIKE '%@demo.ob.local'
 );
DELETE FROM opportunity_domains
 WHERE opp_id IN (
   SELECT o.opp_id FROM opportunities o
   JOIN users u ON u.user_id = o.created_by
   WHERE u.email LIKE '%@demo.ob.local'
 );
-- V5+ hồ sơ tin / org (nếu bảng đã có)
DELETE FROM opportunity_documents
 WHERE opp_id IN (
   SELECT o.opp_id FROM opportunities o
   JOIN users u ON u.user_id = o.created_by
   WHERE u.email LIKE '%@demo.ob.local'
 );
DELETE FROM opportunities
 WHERE created_by IN (SELECT user_id FROM users WHERE email LIKE '%@demo.ob.local');
DELETE FROM org_documents
 WHERE org_id IN (
   SELECT org.org_id FROM organizations org
   JOIN users u ON u.user_id = org.owner_user_id
   WHERE u.email LIKE '%@demo.ob.local'
 );
DELETE FROM org_members
 WHERE user_id IN (SELECT user_id FROM users WHERE email LIKE '%@demo.ob.local');
DELETE FROM organizations
 WHERE owner_user_id IN (SELECT user_id FROM users WHERE email LIKE '%@demo.ob.local');
DELETE FROM student_profiles
 WHERE user_id IN (SELECT user_id FROM users WHERE email LIKE '%@demo.ob.local');
DELETE FROM users WHERE email LIKE '%@demo.ob.local';

-- 2) Domains (tạo nếu chưa có; dùng domain_id cố định khi insert mới)
INSERT INTO domains (domain_id, domain_name)
SELECT v.domain_id, v.domain_name
FROM (VALUES
  ('d0000000-0000-4000-8000-000000000001'::uuid, 'IT'),
  ('d0000000-0000-4000-8000-000000000002'::uuid, 'Marketing'),
  ('d0000000-0000-4000-8000-000000000003'::uuid, 'Tài chính'),
  ('d0000000-0000-4000-8000-000000000004'::uuid, 'Thiết kế'),
  ('d0000000-0000-4000-8000-000000000005'::uuid, 'Khởi nghiệp')
) AS v(domain_id, domain_name)
WHERE NOT EXISTS (SELECT 1 FROM domains d WHERE d.domain_name = v.domain_name);

-- 3) Users (bcrypt of password123 — Spring BCrypt compatible)
-- hash: $2a$10$oI9AP2uqyzfEtDQbj0kjGuoNCE2gUKVvZG4djBKMYXin/VuiOMcPe
INSERT INTO users (
  user_id, email, password_hash, full_name, role, status,
  auth_provider, email_verified_at, created_at, updated_at
) VALUES
  ('a0000000-0000-4000-8000-000000000001', 'admin@demo.ob.local',
   '$2a$10$oI9AP2uqyzfEtDQbj0kjGuoNCE2gUKVvZG4djBKMYXin/VuiOMcPe',
   'Admin Demo', 'ADMIN', 'ACTIVE', 'EMAIL', NOW(), NOW() - INTERVAL '30 days', NOW()),
  ('a0000000-0000-4000-8000-000000000002', 'sv1@demo.ob.local',
   '$2a$10$oI9AP2uqyzfEtDQbj0kjGuoNCE2gUKVvZG4djBKMYXin/VuiOMcPe',
   'Nguyễn Minh An', 'STUDENT', 'ACTIVE', 'EMAIL', NOW(), NOW() - INTERVAL '20 days', NOW()),
  ('a0000000-0000-4000-8000-000000000003', 'sv2@demo.ob.local',
   '$2a$10$oI9AP2uqyzfEtDQbj0kjGuoNCE2gUKVvZG4djBKMYXin/VuiOMcPe',
   'Trần Thu Hà', 'STUDENT', 'ACTIVE', 'EMAIL', NOW(), NOW() - INTERVAL '18 days', NOW()),
  ('a0000000-0000-4000-8000-000000000004', 'provider1@demo.ob.local',
   '$2a$10$oI9AP2uqyzfEtDQbj0kjGuoNCE2gUKVvZG4djBKMYXin/VuiOMcPe',
   'Lê Quốc Bảo', 'PROVIDER', 'ACTIVE', 'EMAIL', NOW(), NOW() - INTERVAL '25 days', NOW()),
  ('a0000000-0000-4000-8000-000000000005', 'provider2@demo.ob.local',
   '$2a$10$oI9AP2uqyzfEtDQbj0kjGuoNCE2gUKVvZG4djBKMYXin/VuiOMcPe',
   'Phạm Ngọc Mai', 'PROVIDER', 'ACTIVE', 'EMAIL', NOW(), NOW() - INTERVAL '22 days', NOW());

-- 4) Student profiles
INSERT INTO student_profiles (profile_id, user_id, major, university, university_year, cv_url, skills, bio)
VALUES
  ('b0000000-0000-4000-8000-000000000002', 'a0000000-0000-4000-8000-000000000002',
   'Công nghệ thông tin', 'Đại học Bách khoa Hà Nội', 3,
   'https://placehold.co/600x800/0388ED/FFFFFF/png?text=CV+Minh+An',
   '["Java","Spring Boot","React","SQL"]'::jsonb,
   'Sinh viên năm 3, thích backend và hackathon.'),
  ('b0000000-0000-4000-8000-000000000003', 'a0000000-0000-4000-8000-000000000003',
   'Marketing', 'Đại học Kinh tế TP.HCM', 2,
   'https://placehold.co/600x800/F69022/FFFFFF/png?text=CV+Thu+Ha',
   '["Content","Canva","SEO","English"]'::jsonb,
   'Quan tâm học bổng và startup marketing.');

-- 5) Organizations
INSERT INTO organizations (
  org_id, owner_user_id, org_name, logo_url, website, contact_email, contact_phone,
  description, verified_status, verified_at, verified_by, created_at, updated_at
) VALUES
  ('c0000000-0000-4000-8000-000000000001', 'a0000000-0000-4000-8000-000000000004',
   'FPT Software Academy',
   'https://api.dicebear.com/9.x/initials/svg?seed=FPT&backgroundColor=0388ed&textColor=ffffff',
   'https://www.fpt-software.com',
   'provider1@demo.ob.local', '0901001001',
   'Đơn vị đào tạo và tuyển dụng thực tập sinh IT toàn quốc.',
   'VERIFIED', NOW() - INTERVAL '10 days', 'a0000000-0000-4000-8000-000000000001',
   NOW() - INTERVAL '25 days', NOW()),
  ('c0000000-0000-4000-8000-000000000002', 'a0000000-0000-4000-8000-000000000005',
   'Startup Hub Vietnam',
   'https://api.dicebear.com/9.x/initials/svg?seed=SHV&backgroundColor=f69022&textColor=ffffff',
   'https://example.com/startup-hub',
   'provider2@demo.ob.local', '0902002002',
   'Cộng đồng hỗ trợ khởi nghiệp sinh viên: hackathon, quỹ, ươm tạo.',
   'VERIFIED', NOW() - INTERVAL '8 days', 'a0000000-0000-4000-8000-000000000001',
   NOW() - INTERVAL '22 days', NOW());

INSERT INTO org_members (org_member_id, org_id, user_id, member_role) VALUES
  ('e0000000-0000-4000-8000-000000000001', 'c0000000-0000-4000-8000-000000000001',
   'a0000000-0000-4000-8000-000000000004', 'OWNER'),
  ('e0000000-0000-4000-8000-000000000002', 'c0000000-0000-4000-8000-000000000002',
   'a0000000-0000-4000-8000-000000000005', 'OWNER');

-- 5b) Hồ sơ tổ chức (AI lớp 1) — placeholder URL
INSERT INTO org_documents (doc_id, org_id, doc_type, title, file_url, created_at) VALUES
  ('0d000000-0000-4000-8000-000000000001', 'c0000000-0000-4000-8000-000000000001',
   'BUSINESS_LICENSE', 'GPKD FPT Software Academy (demo)',
   'https://placehold.co/800x1100/0388ED/FFFFFF/png?text=GPKD+FPT', NOW()),
  ('0d000000-0000-4000-8000-000000000002', 'c0000000-0000-4000-8000-000000000001',
   'TAX_CODE', 'MST demo FPT',
   'https://placehold.co/800x600/0d9488/FFFFFF/png?text=MST+0101234567', NOW()),
  ('0d000000-0000-4000-8000-000000000003', 'c0000000-0000-4000-8000-000000000002',
   'BUSINESS_LICENSE', 'GPKD Startup Hub (demo)',
   'https://placehold.co/800x1100/F69022/FFFFFF/png?text=GPKD+SHV', NOW());

-- 6) Opportunities (7 categories, APPROVED + 1 PENDING + 1 featured)
-- created_at phải < deadline (constraint chk_deadline_future)
WITH cats AS (
  SELECT code, category_id FROM categories
)
INSERT INTO opportunities (
  opp_id, org_id, created_by, category_id, title, slug,
  logo_url, banner_url, description, requirements, benefits,
  salary_or_reward, selection_process,
  location, work_type, apply_mode, external_link,
  deadline, status, moderated_by, moderated_at,
  is_featured, featured_by, featured_at, featured_until,
  view_count, bookmark_count, application_count, share_count,
  published_at, created_at, updated_at
)
SELECT * FROM (VALUES
  -- 1 INTERNSHIP
  ('f0000000-0000-4000-8000-000000000001'::uuid,
   'c0000000-0000-4000-8000-000000000001'::uuid,
   'a0000000-0000-4000-8000-000000000004'::uuid,
   (SELECT category_id FROM cats WHERE code = 'INTERNSHIP'),
   'Thực tập Java Backend 2026',
   'thuc-tap-java-backend-2026',
   'https://api.dicebear.com/9.x/icons/svg?seed=java&backgroundColor=0388ed',
   'https://placehold.co/1200x500/0388ED/FFFFFF/png?text=Java+Internship',
   'Chương trình thực tập 3–6 tháng tại FPT Software Academy. Mentor 1-1, làm dự án thật.',
   'Sinh viên năm 3–4; biết Java OOP; ưu tiên Spring Boot.',
   'Trợ cấp, mentorship, cơ hội nhận full-time.',
   '5–7 triệu/tháng',
   'CV screening → Technical test → Interview → Offer',
   'HA_NOI'::location_type, 'HYBRID'::work_type, 'INTERNAL'::apply_mode, NULL,
   NOW() + INTERVAL '45 days', 'APPROVED'::opp_status,
   'a0000000-0000-4000-8000-000000000001'::uuid, NOW() - INTERVAL '5 days',
   TRUE, 'a0000000-0000-4000-8000-000000000001'::uuid, NOW() - INTERVAL '5 days', NOW() + INTERVAL '30 days',
   128, 12, 2, 5,
   NOW() - INTERVAL '5 days', NOW() - INTERVAL '12 days', NOW()),

  -- 2 STARTUP
  ('f0000000-0000-4000-8000-000000000002'::uuid,
   'c0000000-0000-4000-8000-000000000002'::uuid,
   'a0000000-0000-4000-8000-000000000005'::uuid,
   (SELECT category_id FROM cats WHERE code = 'STARTUP_RECRUITMENT'),
   'Tuyển CTV Growth Marketing cho Startup EdTech',
   'ctv-growth-marketing-edtech',
   'https://api.dicebear.com/9.x/icons/svg?seed=marketing&backgroundColor=f69022',
   'https://placehold.co/1200x500/F69022/FFFFFF/png?text=Startup+Marketing',
   'Startup EdTech tìm CTV Growth: content, community, performance ads.',
   'Có portfolio content; biết Meta Ads là lợi thế.',
   'Thưởng theo KPI, môi trường startup, flexible.',
   'Thỏa thuận theo KPI',
   'Nộp form → Phỏng vấn founder → Onboarding',
   'TP_HCM'::location_type, 'ONLINE'::work_type, 'EXTERNAL'::apply_mode,
   'https://example.com/apply/edtech-growth',
   NOW() + INTERVAL '20 days', 'APPROVED'::opp_status,
   'a0000000-0000-4000-8000-000000000001'::uuid, NOW() - INTERVAL '3 days',
   FALSE, NULL, NULL, NULL,
   86, 7, 1, 3,
   NOW() - INTERVAL '3 days', NOW() - INTERVAL '9 days', NOW()),

  -- 3 INNOVATION
  ('f0000000-0000-4000-8000-000000000003'::uuid,
   'c0000000-0000-4000-8000-000000000002'::uuid,
   'a0000000-0000-4000-8000-000000000005'::uuid,
   (SELECT category_id FROM cats WHERE code = 'INNOVATION_CONTEST'),
   'Cuộc thi Ý tưởng Khởi nghiệp Sinh viên 2026',
   'cuoc-thi-y-tuong-khoi-nghiep-2026',
   'https://api.dicebear.com/9.x/icons/svg?seed=idea&backgroundColor=fcbd0e',
   'https://placehold.co/1200x500/FCBD0E/1a1a1a/png?text=Innovation+Contest',
   'Sân chơi pitch ý tưởng cho sinh viên toàn quốc. Mentoring từ quỹ đầu tư.',
   'Nhóm 2–5 người; ít nhất 1 thành viên là sinh viên.',
   'Giải thưởng tiền mặt + suất ươm tạo.',
   'Giải nhất 50 triệu',
   'Vòng hồ sơ → Bán kết pitch → Chung kết',
   'TOAN_QUOC'::location_type, 'HYBRID'::work_type, 'EXTERNAL'::apply_mode,
   'https://example.com/contest/startup-idea-2026',
   NOW() + INTERVAL '60 days', 'APPROVED'::opp_status,
   'a0000000-0000-4000-8000-000000000001'::uuid, NOW() - INTERVAL '2 days',
   TRUE, 'a0000000-0000-4000-8000-000000000001'::uuid, NOW() - INTERVAL '2 days', NOW() + INTERVAL '40 days',
   210, 25, 0, 11,
   NOW() - INTERVAL '2 days', NOW() - INTERVAL '8 days', NOW()),

  -- 4 HACKATHON
  ('f0000000-0000-4000-8000-000000000004'::uuid,
   'c0000000-0000-4000-8000-000000000001'::uuid,
   'a0000000-0000-4000-8000-000000000004'::uuid,
   (SELECT category_id FROM cats WHERE code = 'HACKATHON'),
   'OppHack 48h — AI for Campus',
   'opphack-48h-ai-for-campus',
   'https://api.dicebear.com/9.x/icons/svg?seed=hack&backgroundColor=0736ac',
   'https://placehold.co/1200x500/0736AC/FFFFFF/png?text=OppHack+48h',
   'Hackathon 48 giờ xây sản phẩm AI phục vụ đời sống sinh viên.',
   'Biết lập trình web/mobile; mang laptop.',
   'Giải thưởng, swag, cơ hội thực tập FPT.',
   'Tổng giải thưởng 100 triệu',
   'Đăng ký online → Check-in → Hack 48h → Demo day',
   'DA_NANG'::location_type, 'OFFLINE'::work_type, 'INTERNAL'::apply_mode, NULL,
   NOW() + INTERVAL '15 days', 'APPROVED'::opp_status,
   'a0000000-0000-4000-8000-000000000001'::uuid, NOW() - INTERVAL '4 days',
   TRUE, 'a0000000-0000-4000-8000-000000000001'::uuid, NOW() - INTERVAL '4 days', NOW() + INTERVAL '14 days',
   340, 40, 1, 18,
   NOW() - INTERVAL '4 days', NOW() - INTERVAL '10 days', NOW()),

  -- 5 SCHOLARSHIP
  ('f0000000-0000-4000-8000-000000000005'::uuid,
   'c0000000-0000-4000-8000-000000000002'::uuid,
   'a0000000-0000-4000-8000-000000000005'::uuid,
   (SELECT category_id FROM cats WHERE code = 'SCHOLARSHIP'),
   'Học bổng Khuyến học Startup Hub 2026',
   'hoc-bong-khuyen-hoc-startup-hub-2026',
   'https://api.dicebear.com/9.x/icons/svg?seed=scholarship&backgroundColor=16a34a',
   'https://placehold.co/1200x500/16A34A/FFFFFF/png?text=Scholarship+2026',
   'Học bổng hỗ trợ sinh viên xuất sắc có dự án cộng đồng/khởi nghiệp.',
   'GPA >= 3.2; có bài luận 500–800 từ.',
   'Hỗ trợ học phí + mentoring.',
   '15 triệu / suất',
   'Nộp hồ sơ → Phỏng vấn → Công bố',
   'TOAN_QUOC'::location_type, 'ONLINE'::work_type, 'INTERNAL'::apply_mode, NULL,
   NOW() + INTERVAL '35 days', 'APPROVED'::opp_status,
   'a0000000-0000-4000-8000-000000000001'::uuid, NOW() - INTERVAL '6 days',
   FALSE, NULL, NULL, NULL,
   95, 15, 1, 4,
   NOW() - INTERVAL '6 days', NOW() - INTERVAL '14 days', NOW()),

  -- 6 INVESTMENT
  ('f0000000-0000-4000-8000-000000000006'::uuid,
   'c0000000-0000-4000-8000-000000000002'::uuid,
   'a0000000-0000-4000-8000-000000000005'::uuid,
   (SELECT category_id FROM cats WHERE code = 'INVESTMENT_FUND'),
   'Quỹ hạt giống Sinh viên — Seed Call Q3',
   'quy-hat-giong-sinh-vien-q3',
   'https://api.dicebear.com/9.x/icons/svg?seed=fund&backgroundColor=7c3aed',
   'https://placehold.co/1200x500/7C3AED/FFFFFF/png?text=Student+Seed+Fund',
   'Gọi vốn hạt giống cho dự án do sinh viên sáng lập (pre-seed).',
   'Có MVP hoặc prototype; team >= 2.',
   'Vốn + coaching từ quỹ đối tác.',
   'Ticket 200–500 triệu (equity)',
   'Pitch deck → Screening → Partner meeting',
   'TP_HCM'::location_type, 'HYBRID'::work_type, 'EXTERNAL'::apply_mode,
   'https://example.com/funds/student-seed-q3',
   NOW() + INTERVAL '50 days', 'APPROVED'::opp_status,
   'a0000000-0000-4000-8000-000000000001'::uuid, NOW() - INTERVAL '1 days',
   FALSE, NULL, NULL, NULL,
   77, 9, 0, 2,
   NOW() - INTERVAL '1 days', NOW() - INTERVAL '7 days', NOW()),

  -- 7 INCUBATOR
  ('f0000000-0000-4000-8000-000000000007'::uuid,
   'c0000000-0000-4000-8000-000000000001'::uuid,
   'a0000000-0000-4000-8000-000000000004'::uuid,
   (SELECT category_id FROM cats WHERE code = 'INCUBATOR'),
   'Chương trình ươm tạo Campus Builders',
   'uom-tao-campus-builders',
   'https://api.dicebear.com/9.x/icons/svg?seed=incubator&backgroundColor=0ea5e9',
   'https://placehold.co/1200x500/0EA5E9/FFFFFF/png?text=Campus+Builders',
   'Khóa ươm tạo 12 tuần: product, GTM, fundraising basics.',
   'Đang học đại học; có ý tưởng hoặc early startup.',
   'Workspace, mentor, demo day với nhà đầu tư.',
   'Miễn phí tham gia',
   'Apply → Interview → Cohort onboarding',
   'HA_NOI'::location_type, 'OFFLINE'::work_type, 'INTERNAL'::apply_mode, NULL,
   NOW() + INTERVAL '28 days', 'APPROVED'::opp_status,
   'a0000000-0000-4000-8000-000000000001'::uuid, NOW() - INTERVAL '7 days',
   FALSE, NULL, NULL, NULL,
   64, 8, 0, 1,
   NOW() - INTERVAL '7 days', NOW() - INTERVAL '15 days', NOW()),

  -- 8 PENDING (chờ admin duyệt)
  ('f0000000-0000-4000-8000-000000000008'::uuid,
   'c0000000-0000-4000-8000-000000000001'::uuid,
   'a0000000-0000-4000-8000-000000000004'::uuid,
   (SELECT category_id FROM cats WHERE code = 'INTERNSHIP'),
   'Thực tập QA Automation (chờ duyệt)',
   'thuc-tap-qa-automation-pending',
   'https://api.dicebear.com/9.x/icons/svg?seed=qa&backgroundColor=64748b',
   'https://placehold.co/1200x500/64748B/FFFFFF/png?text=QA+Internship+Pending',
   'Tin mẫu đang ở trạng thái PENDING để test moderation queue.',
   'Biết Selenium hoặc Playwright cơ bản.',
   'Trợ cấp + mentor QA.',
   '4–6 triệu/tháng',
   'CV → Test case exercise → Interview',
   'TOAN_QUOC'::location_type, 'ONLINE'::work_type, 'INTERNAL'::apply_mode, NULL,
   NOW() + INTERVAL '40 days', 'PENDING'::opp_status,
   NULL, NULL,
   FALSE, NULL, NULL, NULL,
   3, 0, 0, 0,
   NULL, NOW() - INTERVAL '1 days', NOW())
) AS v(
  opp_id, org_id, created_by, category_id, title, slug,
  logo_url, banner_url, description, requirements, benefits,
  salary_or_reward, selection_process,
  location, work_type, apply_mode, external_link,
  deadline, status, moderated_by, moderated_at,
  is_featured, featured_by, featured_at, featured_until,
  view_count, bookmark_count, application_count, share_count,
  published_at, created_at, updated_at
);

-- 7) Opp ↔ Domains (lookup theo tên — an toàn nếu domain_id khác)
INSERT INTO opportunity_domains (opp_id, domain_id)
SELECT opp, d.domain_id
FROM (VALUES
  ('f0000000-0000-4000-8000-000000000001'::uuid, 'IT'),
  ('f0000000-0000-4000-8000-000000000002'::uuid, 'Marketing'),
  ('f0000000-0000-4000-8000-000000000003'::uuid, 'Khởi nghiệp'),
  ('f0000000-0000-4000-8000-000000000004'::uuid, 'IT'),
  ('f0000000-0000-4000-8000-000000000004'::uuid, 'Thiết kế'),
  ('f0000000-0000-4000-8000-000000000005'::uuid, 'Khởi nghiệp'),
  ('f0000000-0000-4000-8000-000000000006'::uuid, 'Tài chính'),
  ('f0000000-0000-4000-8000-000000000007'::uuid, 'Khởi nghiệp')
) AS x(opp, domain_name)
JOIN domains d ON d.domain_name = x.domain_name
ON CONFLICT DO NOTHING;

-- 7b) Hồ sơ tin đăng (AI lớp 2) — vài tin demo chính
INSERT INTO opportunity_documents (doc_id, opp_id, doc_type, title, file_url, created_at) VALUES
  ('0e000000-0000-4000-8000-000000000001', 'f0000000-0000-4000-8000-000000000001',
   'PROGRAM_PROOF', 'Mô tả chương trình thực tập Java (demo)',
   'https://placehold.co/800x1100/0388ED/FFFFFF/png?text=Program+Java+Intern', NOW()),
  ('0e000000-0000-4000-8000-000000000002', 'f0000000-0000-4000-8000-000000000004',
   'PARTNERSHIP_LETTER', 'Thư hợp tác OppHack (demo)',
   'https://placehold.co/800x1100/0736ac/FFFFFF/png?text=Partnership+Hackathon', NOW()),
  ('0e000000-0000-4000-8000-000000000003', 'f0000000-0000-4000-8000-000000000002',
   'PROGRAM_PROOF', 'JD Marketing Intern (demo)',
   'https://placehold.co/800x1100/F69022/FFFFFF/png?text=Marketing+JD', NOW());

-- 8) Applications + history
INSERT INTO applications (
  app_id, opp_id, student_id, is_external, cv_file, cover_letter, status,
  applied_at, reviewed_at, updated_at, updated_by
) VALUES
  ('aa000000-0000-4000-8000-000000000001',
   'f0000000-0000-4000-8000-000000000001', 'a0000000-0000-4000-8000-000000000002',
   FALSE,
   'https://placehold.co/600x800/0388ED/FFFFFF/png?text=CV+Minh+An',
   'Em rất quan tâm vị trí Java Backend.',
   'REVIEWING', NOW() - INTERVAL '2 days', NOW() - INTERVAL '1 days', NOW(),
   'a0000000-0000-4000-8000-000000000004'),
  ('aa000000-0000-4000-8000-000000000002',
   'f0000000-0000-4000-8000-000000000004', 'a0000000-0000-4000-8000-000000000002',
   FALSE,
   'https://placehold.co/600x800/0388ED/FFFFFF/png?text=CV+Minh+An',
   'Em muốn tham gia OppHack 48h.',
   'SUBMITTED', NOW() - INTERVAL '1 days', NULL, NOW(), NULL),
  ('aa000000-0000-4000-8000-000000000003',
   'f0000000-0000-4000-8000-000000000005', 'a0000000-0000-4000-8000-000000000003',
   FALSE,
   'https://placehold.co/600x800/F69022/FFFFFF/png?text=CV+Thu+Ha',
   'Em ứng tuyển học bổng khuyến học.',
   'SUBMITTED', NOW() - INTERVAL '12 hours', NULL, NOW(), NULL),
  ('aa000000-0000-4000-8000-000000000004',
   'f0000000-0000-4000-8000-000000000002', 'a0000000-0000-4000-8000-000000000003',
   TRUE, NULL, 'Đã click link ngoài.',
   'SUBMITTED', NOW() - INTERVAL '6 hours', NULL, NOW(), NULL);

INSERT INTO application_status_history (id, app_id, from_status, to_status, changed_by, note, created_at) VALUES
  ('af000000-0000-4000-8000-000000000001',
   'aa000000-0000-4000-8000-000000000001', 'SUBMITTED', 'REVIEWING',
   'a0000000-0000-4000-8000-000000000004', 'Chuyển sang đang xét', NOW() - INTERVAL '1 days');

-- 9) Bookmarks
INSERT INTO saved_opportunities (id, student_id, opp_id, notify_before_hours, saved_at) VALUES
  ('ab000000-0000-4000-8000-000000000001',
   'a0000000-0000-4000-8000-000000000002', 'f0000000-0000-4000-8000-000000000003', 48, NOW() - INTERVAL '1 days'),
  ('ab000000-0000-4000-8000-000000000002',
   'a0000000-0000-4000-8000-000000000002', 'f0000000-0000-4000-8000-000000000004', 24, NOW() - INTERVAL '2 days'),
  ('ab000000-0000-4000-8000-000000000003',
   'a0000000-0000-4000-8000-000000000003', 'f0000000-0000-4000-8000-000000000005', 48, NOW() - INTERVAL '3 days');

-- 10) Notifications
INSERT INTO notifications (notification_id, user_id, type, channel, title, body, ref_id, is_read, sent_at, created_at) VALUES
  ('ac000000-0000-4000-8000-000000000001',
   'a0000000-0000-4000-8000-000000000002', 'APP_STATUS', 'IN_APP',
   'Hồ sơ đang được xét', 'Đơn Java Backend đã chuyển sang REVIEWING.',
   'aa000000-0000-4000-8000-000000000001', FALSE, NOW() - INTERVAL '1 days', NOW() - INTERVAL '1 days'),
  ('ac000000-0000-4000-8000-000000000002',
   'a0000000-0000-4000-8000-000000000001', 'PENDING_REVIEW', 'IN_APP',
   'Tin mới chờ duyệt', 'Có tin Thực tập QA Automation cần moderation.',
   'f0000000-0000-4000-8000-000000000008', FALSE, NOW() - INTERVAL '1 days', NOW() - INTERVAL '1 days'),
  ('ac000000-0000-4000-8000-000000000003',
   'a0000000-0000-4000-8000-000000000003', 'NEW_OPP', 'IN_APP',
   'Học bổng mới', 'Học bổng Khuyến học Startup Hub 2026 vừa đăng.',
   'f0000000-0000-4000-8000-000000000005', TRUE, NOW() - INTERVAL '6 days', NOW() - INTERVAL '6 days');

INSERT INTO notification_preferences (user_id, type, channel, enabled, frequency, categories, domains) VALUES
  ('a0000000-0000-4000-8000-000000000002', 'NEW_OPP', 'IN_APP', TRUE, 'INSTANT', '[]'::jsonb, '[]'::jsonb),
  ('a0000000-0000-4000-8000-000000000002', 'DEADLINE_ALERT', 'IN_APP', TRUE, 'INSTANT', '[]'::jsonb, '[]'::jsonb),
  ('a0000000-0000-4000-8000-000000000003', 'NEW_OPP', 'IN_APP', TRUE, 'DAILY_DIGEST', '[]'::jsonb, '[]'::jsonb)
ON CONFLICT DO NOTHING;

-- 11) Moderation logs cho tin đã duyệt
INSERT INTO moderation_logs (id, opp_id, admin_id, action, reason, created_at) VALUES
  ('ad000000-0000-4000-8000-000000000001',
   'f0000000-0000-4000-8000-000000000001', 'a0000000-0000-4000-8000-000000000001',
   'APPROVED', 'Nội dung hợp lệ', NOW() - INTERVAL '5 days'),
  ('ad000000-0000-4000-8000-000000000002',
   'f0000000-0000-4000-8000-000000000004', 'a0000000-0000-4000-8000-000000000001',
   'APPROVED', 'Hackathon OK', NOW() - INTERVAL '4 days');

-- Tài khoản demo (password: password123)
-- admin@demo.ob.local
-- sv1@demo.ob.local / sv2@demo.ob.local
-- provider1@demo.ob.local / provider2@demo.ob.local
