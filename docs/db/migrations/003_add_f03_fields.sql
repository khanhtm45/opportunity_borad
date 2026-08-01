-- F03/F04 enrichment: banner, lương/giải thưởng, quy trình tuyển chọn,
-- liên hệ org, share_count. Chạy trên Supabase/Postgres đã có schema.
ALTER TABLE opportunities ADD COLUMN IF NOT EXISTS banner_url VARCHAR(512);
ALTER TABLE opportunities ADD COLUMN IF NOT EXISTS salary_or_reward TEXT;
ALTER TABLE opportunities ADD COLUMN IF NOT EXISTS selection_process TEXT;
ALTER TABLE opportunities ADD COLUMN IF NOT EXISTS share_count INT NOT NULL DEFAULT 0;

ALTER TABLE organizations ADD COLUMN IF NOT EXISTS contact_email VARCHAR(255);
ALTER TABLE organizations ADD COLUMN IF NOT EXISTS contact_phone VARCHAR(40);

COMMENT ON COLUMN opportunities.banner_url IS 'Banner/card image (F01/F03); khác logo_url';
COMMENT ON COLUMN opportunities.salary_or_reward IS 'Quyền lợi / mức lương / giải thưởng (F03)';
COMMENT ON COLUMN opportunities.selection_process IS 'Quy trình tuyển chọn / lịch trình (F03)';
COMMENT ON COLUMN opportunities.share_count IS 'Số lần chia sẻ (F04.3)';
COMMENT ON COLUMN organizations.contact_email IS 'Email liên hệ đơn vị đăng tin (F03)';
COMMENT ON COLUMN organizations.contact_phone IS 'SĐT liên hệ đơn vị đăng tin (F03)';
