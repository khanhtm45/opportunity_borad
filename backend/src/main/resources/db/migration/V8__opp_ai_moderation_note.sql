-- Ghi chú AI/Admin khi yêu cầu provider cập nhật tin đăng
ALTER TABLE opportunities
    ADD COLUMN IF NOT EXISTS ai_moderation_note TEXT,
    ADD COLUMN IF NOT EXISTS ai_scanned_at TIMESTAMP;

DO $$ BEGIN
    ALTER TYPE notif_type ADD VALUE IF NOT EXISTS 'OPP_UPDATE_REQUIRED';
EXCEPTION WHEN duplicate_object THEN NULL;
END $$;
