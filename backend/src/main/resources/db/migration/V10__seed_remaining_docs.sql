-- Bổ sung hồ sơ còn thiếu cho demo (idempotent) — chạy sau V3 + V5–V9
-- Opp docs cho tin chưa có; tax_code org; thêm đơn SUBMITTED để test AI scan CV

-- Org tax / địa chỉ
UPDATE organizations SET
  tax_code = COALESCE(NULLIF(tax_code, ''), '0101234567'),
  address = COALESCE(NULLIF(address, ''), 'Tòa FPT, Cầu Giấy, Hà Nội'),
  industry = COALESCE(NULLIF(industry, ''), 'Phần mềm / Đào tạo'),
  company_size = COALESCE(company_size, 'SIZE_201_500'::company_size),
  updated_at = NOW()
WHERE org_id = 'c0000000-0000-4000-8000-000000000001';

UPDATE organizations SET
  tax_code = COALESCE(NULLIF(tax_code, ''), '0312345678'),
  address = COALESCE(NULLIF(address, ''), 'Quận 1, TP.HCM'),
  industry = COALESCE(NULLIF(industry, ''), 'Khởi nghiệp / Cộng đồng'),
  company_size = COALESCE(company_size, 'SIZE_11_50'::company_size),
  updated_at = NOW()
WHERE org_id = 'c0000000-0000-4000-8000-000000000002';

-- Hồ sơ thuế còn thiếu cho Startup Hub
INSERT INTO org_documents (doc_id, org_id, doc_type, title, file_url, created_at)
SELECT '0d000000-0000-4000-8000-000000000004',
       'c0000000-0000-4000-8000-000000000002',
       'TAX_CODE', 'MST demo Startup Hub',
       'https://placehold.co/800x600/F69022/FFFFFF/png?text=MST+0312345678', NOW()
WHERE NOT EXISTS (
  SELECT 1 FROM org_documents WHERE doc_id = '0d000000-0000-4000-8000-000000000004'
);

-- Hồ sơ tin còn thiếu (các opp demo chưa có document)
INSERT INTO opportunity_documents (doc_id, opp_id, doc_type, title, file_url, created_at)
SELECT v.doc_id, v.opp_id, v.doc_type::opp_doc_type, v.title, v.file_url, NOW()
FROM (VALUES
  ('0e000000-0000-4000-8000-000000000004'::uuid, 'f0000000-0000-4000-8000-000000000003'::uuid,
   'PROGRAM_PROOF', 'Thể lệ cuộc thi ý tưởng (demo)',
   'https://placehold.co/800x1100/fcbd0e/FFFFFF/png?text=Contest+Rules'),
  ('0e000000-0000-4000-8000-000000000005'::uuid, 'f0000000-0000-4000-8000-000000000005'::uuid,
   'PROGRAM_PROOF', 'Điều kiện học bổng (demo)',
   'https://placehold.co/800x1100/16a34a/FFFFFF/png?text=Scholarship+Terms'),
  ('0e000000-0000-4000-8000-000000000006'::uuid, 'f0000000-0000-4000-8000-000000000006'::uuid,
   'PROGRAM_PROOF', 'Call for proposal quỹ hạt giống (demo)',
   'https://placehold.co/800x1100/7c3aed/FFFFFF/png?text=Seed+Fund+Call'),
  ('0e000000-0000-4000-8000-000000000007'::uuid, 'f0000000-0000-4000-8000-000000000007'::uuid,
   'PARTNERSHIP_LETTER', 'Thư mời ươm tạo Campus Builders (demo)',
   'https://placehold.co/800x1100/0ea5e9/FFFFFF/png?text=Incubator+Invite'),
  ('0e000000-0000-4000-8000-000000000008'::uuid, 'f0000000-0000-4000-8000-000000000008'::uuid,
   'PROGRAM_PROOF', 'JD QA Automation chờ duyệt (demo)',
   'https://placehold.co/800x1100/64748b/FFFFFF/png?text=QA+JD+Pending')
) AS v(doc_id, opp_id, doc_type, title, file_url)
WHERE EXISTS (SELECT 1 FROM opportunities o WHERE o.opp_id = v.opp_id)
  AND NOT EXISTS (SELECT 1 FROM opportunity_documents d WHERE d.doc_id = v.doc_id);

-- Thêm đơn SUBMITTED để Provider test AI scan CV (idempotent)
INSERT INTO applications (
  app_id, opp_id, student_id, is_external, cv_file, cover_letter, status,
  applied_at, reviewed_at, updated_at, updated_by
)
SELECT * FROM (VALUES
  ('aa000000-0000-4000-8000-000000000005'::uuid,
   'f0000000-0000-4000-8000-000000000001'::uuid,
   'a0000000-0000-4000-8000-000000000003'::uuid,
   FALSE,
   'https://placehold.co/600x800/F69022/FFFFFF/png?text=CV+Thu+Ha',
   'Em muốn chuyển sang học Java backend.',
   'SUBMITTED'::app_status, NOW() - INTERVAL '3 hours', NULL::timestamp, NOW(), NULL::uuid),
  ('aa000000-0000-4000-8000-000000000006'::uuid,
   'f0000000-0000-4000-8000-000000000007'::uuid,
   'a0000000-0000-4000-8000-000000000002'::uuid,
   FALSE,
   'https://placehold.co/600x800/0388ED/FFFFFF/png?text=CV+Minh+An',
   'Em quan tâm chương trình ươm tạo.',
   'SUBMITTED'::app_status, NOW() - INTERVAL '2 hours', NULL::timestamp, NOW(), NULL::uuid)
) AS v(app_id, opp_id, student_id, is_external, cv_file, cover_letter, status,
       applied_at, reviewed_at, updated_at, updated_by)
WHERE EXISTS (SELECT 1 FROM opportunities o WHERE o.opp_id = v.opp_id)
  AND EXISTS (SELECT 1 FROM users u WHERE u.user_id = v.student_id)
  AND NOT EXISTS (SELECT 1 FROM applications a WHERE a.app_id = v.app_id)
ON CONFLICT DO NOTHING;

-- Cập nhật application_count cho tin vừa thêm đơn
UPDATE opportunities o SET
  application_count = (SELECT count(*) FROM applications a WHERE a.opp_id = o.opp_id),
  updated_at = NOW()
WHERE o.opp_id IN (
  'f0000000-0000-4000-8000-000000000001',
  'f0000000-0000-4000-8000-000000000007'
);
