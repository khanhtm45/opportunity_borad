-- Khi AI/Admin phát hiện hồ sơ sai/thiếu → yêu cầu provider cập nhật lại
DO $$ BEGIN
    ALTER TYPE org_verified ADD VALUE IF NOT EXISTS 'NEEDS_UPDATE';
EXCEPTION WHEN duplicate_object THEN NULL;
END $$;

DO $$ BEGIN
    ALTER TYPE notif_type ADD VALUE IF NOT EXISTS 'ORG_UPDATE_REQUIRED';
EXCEPTION WHEN duplicate_object THEN NULL;
END $$;

ALTER TABLE organizations
    ADD COLUMN IF NOT EXISTS verification_note TEXT,
    ADD COLUMN IF NOT EXISTS ai_scanned_at TIMESTAMP;
