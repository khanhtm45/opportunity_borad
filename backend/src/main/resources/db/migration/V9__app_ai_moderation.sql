-- AI quét hồ sơ ứng tuyển (CV sinh viên) — nhà đăng / admin xem lại rồi gửi yêu cầu cập nhật
ALTER TABLE applications
    ADD COLUMN IF NOT EXISTS ai_moderation_note TEXT,
    ADD COLUMN IF NOT EXISTS ai_scanned_at TIMESTAMP,
    ADD COLUMN IF NOT EXISTS screening_criteria TEXT;

DO $$ BEGIN
    ALTER TYPE notif_type ADD VALUE IF NOT EXISTS 'APP_UPDATE_REQUIRED';
EXCEPTION WHEN duplicate_object THEN NULL;
END $$;
