-- Bổ sung cột F03/F04 cho DB đã tạo từ V1 cũ (idempotent).
ALTER TABLE opportunities ADD COLUMN IF NOT EXISTS logo_url VARCHAR(512);
ALTER TABLE opportunities ADD COLUMN IF NOT EXISTS banner_url VARCHAR(512);
ALTER TABLE opportunities ADD COLUMN IF NOT EXISTS salary_or_reward TEXT;
ALTER TABLE opportunities ADD COLUMN IF NOT EXISTS selection_process TEXT;
ALTER TABLE opportunities ADD COLUMN IF NOT EXISTS share_count INT NOT NULL DEFAULT 0;

ALTER TABLE organizations ADD COLUMN IF NOT EXISTS contact_email VARCHAR(255);
ALTER TABLE organizations ADD COLUMN IF NOT EXISTS contact_phone VARCHAR(40);
