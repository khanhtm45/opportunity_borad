-- Bổ sung mã tham chiếu bên thứ 3 (external reference / "case index ngoài")
-- cho cơ hội được "pitch" từ hệ thống ngoài. Idempotent, chạy an toàn trên DB cũ.
ALTER TABLE opportunities ADD COLUMN IF NOT EXISTS external_ref VARCHAR(120);

-- Index nhẹ để tra cứu theo mã tham chiếu (tùy chọn, giúp admin/provider tìm nhanh)
CREATE INDEX IF NOT EXISTS idx_opps_external_ref ON opportunities(external_ref);
