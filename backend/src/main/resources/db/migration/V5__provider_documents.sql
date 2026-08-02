-- Hồ sơ xác minh tổ chức + hồ sơ liên quan từng tin đăng (URL)
-- Idempotent: an toàn khi chạy lại trên DB đã có một phần schema.
DO $$ BEGIN
    CREATE TYPE org_doc_type AS ENUM ('BUSINESS_LICENSE', 'TAX_CODE', 'IDENTITY', 'OTHER');
EXCEPTION WHEN duplicate_object THEN NULL;
END $$;

DO $$ BEGIN
    CREATE TYPE opp_doc_type AS ENUM ('PROGRAM_PROOF', 'PARTNERSHIP_LETTER', 'OTHER');
EXCEPTION WHEN duplicate_object THEN NULL;
END $$;

CREATE TABLE IF NOT EXISTS org_documents (
    doc_id     UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    org_id     UUID NOT NULL REFERENCES organizations(org_id) ON DELETE CASCADE,
    doc_type   org_doc_type NOT NULL,
    title      VARCHAR(200) NOT NULL,
    file_url   VARCHAR(512) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT NOW()
);
CREATE INDEX IF NOT EXISTS idx_org_documents_org ON org_documents(org_id);

CREATE TABLE IF NOT EXISTS opportunity_documents (
    doc_id     UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    opp_id     UUID NOT NULL REFERENCES opportunities(opp_id) ON DELETE CASCADE,
    doc_type   opp_doc_type NOT NULL,
    title      VARCHAR(200) NOT NULL,
    file_url   VARCHAR(512) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT NOW()
);
CREATE INDEX IF NOT EXISTS idx_opp_documents_opp ON opportunity_documents(opp_id);
