-- Thêm logo_url riêng cho opportunity (ưu tiên hơn org.logo_url)
ALTER TABLE opportunities ADD COLUMN IF NOT EXISTS logo_url TEXT;
COMMENT ON COLUMN opportunities.logo_url IS 'URL logo riêng của cơ hội; fallback org.logo_url nếu null';
