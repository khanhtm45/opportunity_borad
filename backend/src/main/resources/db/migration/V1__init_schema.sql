-- ============================================================================
-- OPPORTUNITY BOARD — DATABASE SCHEMA (PostgreSQL 14+)
-- Tích hợp toàn bộ phát hiện từ Phan_Tich_Chi_Tiet_Lo_Hong.md và Danh_Gia.
-- Chạy: psql -d opportunity_board -f schema.sql
-- ============================================================================

-- pgcrypto handled by migration; assume available   -- cho gen_random_uuid()
CREATE EXTENSION IF NOT EXISTS "pg_trgm";     -- cho full-text / LIKE tối ưu (F02)

-- ---------------------------------------------------------------------------
-- ENUMS
-- ---------------------------------------------------------------------------
CREATE TYPE user_role        AS ENUM ('STUDENT', 'PROVIDER', 'ADMIN');
CREATE TYPE user_status      AS ENUM ('ACTIVE', 'LOCKED', 'PENDING_VERIFICATION');
CREATE TYPE auth_provider    AS ENUM ('EMAIL', 'GOOGLE', 'SSO_SCHOOL');
CREATE TYPE org_verified     AS ENUM ('PENDING', 'VERIFIED', 'REJECTED');

CREATE TYPE opp_status       AS ENUM ('DRAFT', 'PENDING', 'APPROVED', 'REJECTED', 'HIDDEN', 'CLOSED', 'EXPIRED');
CREATE TYPE apply_mode       AS ENUM ('INTERNAL', 'EXTERNAL');
CREATE TYPE work_type        AS ENUM ('ONLINE', 'OFFLINE', 'HYBRID');
CREATE TYPE location_type    AS ENUM ('HA_NOI', 'TP_HCM', 'DA_NANG', 'TOAN_QUOC', 'QUOC_TE', 'KHAC');

CREATE TYPE app_status       AS ENUM ('SUBMITTED', 'REVIEWING', 'INTERVIEW', 'ACCEPTED', 'REJECTED', 'WITHDRAWN');
CREATE TYPE notif_type       AS ENUM ('NEW_OPP', 'DEADLINE_ALERT', 'APP_STATUS', 'PENDING_REVIEW', 'OPP_REJECTED', 'OPP_APPROVED');
CREATE TYPE notif_channel    AS ENUM ('EMAIL', 'PUSH', 'IN_APP');
CREATE TYPE notif_frequency  AS ENUM ('INSTANT', 'DAILY_DIGEST', 'WEEKLY');
CREATE TYPE moderation_action AS ENUM ('APPROVED', 'REJECTED');

-- ---------------------------------------------------------------------------
-- 1. USERS  (A1: bổ sung password_hash, status, last_login, auth_provider)
-- ---------------------------------------------------------------------------
CREATE TABLE users (
    user_id           UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    email             VARCHAR(255) NOT NULL UNIQUE,
    password_hash     VARCHAR(255),                       -- NULL nếu SSO/Google
    full_name         VARCHAR(150) NOT NULL,
    role              user_role NOT NULL,
    status            user_status NOT NULL DEFAULT 'ACTIVE',
    auth_provider     auth_provider NOT NULL DEFAULT 'EMAIL',
    email_verified_at TIMESTAMP,
    last_login_at     TIMESTAMP,
    mfa_enabled       BOOLEAN NOT NULL DEFAULT FALSE,
    failed_login_count SMALLINT NOT NULL DEFAULT 0,        -- chống brute-force
    locked_until      TIMESTAMP,                           -- lockout tạm thời
    password_version  INT NOT NULL DEFAULT 1,              -- revoke token khi đổi mk
    created_at        TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at        TIMESTAMP NOT NULL DEFAULT NOW()
);
CREATE INDEX idx_users_role_status ON users(role, status);
CREATE INDEX idx_users_email_lower ON users(LOWER(email));

-- ---------------------------------------------------------------------------
-- 2. STUDENT_PROFILES
-- ---------------------------------------------------------------------------
CREATE TABLE student_profiles (
    profile_id   UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id      UUID NOT NULL UNIQUE REFERENCES users(user_id) ON DELETE CASCADE,
    major        VARCHAR(120),
    university   VARCHAR(150),
    university_year SMALLINT CHECK (university_year BETWEEN 1 AND 8),
    cv_url       VARCHAR(512),
    skills       JSONB DEFAULT '[]',                       -- ["Java","Figma"]
    bio          TEXT,
    created_at   TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at   TIMESTAMP NOT NULL DEFAULT NOW()
);

-- ---------------------------------------------------------------------------
-- 3. ORGANIZATIONS  (A3: verified workflow)
-- ---------------------------------------------------------------------------
CREATE TABLE organizations (
    org_id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    owner_user_id   UUID NOT NULL REFERENCES users(user_id),
    org_name        VARCHAR(200) NOT NULL,
    logo_url        VARCHAR(512),
    website         VARCHAR(255),
    description     TEXT,
    verified_status org_verified NOT NULL DEFAULT 'PENDING',
    verified_at     TIMESTAMP,
    verified_by     UUID REFERENCES users(user_id),        -- admin duyệt
    created_at      TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMP NOT NULL DEFAULT NOW()
);
CREATE INDEX idx_orgs_verified ON organizations(verified_status);

-- Nhiều thành viên trong 1 org (quyết định: 1 org = nhiều member)
CREATE TABLE org_members (
    org_member_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    org_id        UUID NOT NULL REFERENCES organizations(org_id) ON DELETE CASCADE,
    user_id       UUID NOT NULL REFERENCES users(user_id) ON DELETE CASCADE,
    member_role   VARCHAR(30) NOT NULL DEFAULT 'RECRUITER',  -- OWNER/RECRUITER/MEMBER
    created_at    TIMESTAMP NOT NULL DEFAULT NOW(),
    UNIQUE (org_id, user_id)
);

-- ---------------------------------------------------------------------------
-- 4. CATEGORIES  (Mục 4.1: 7 nhóm; A4: is_system phân biệt fixed/extensible)
-- ---------------------------------------------------------------------------
CREATE TABLE categories (
    category_id   UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    code          VARCHAR(40) NOT NULL UNIQUE,             -- INTERNSHIP, HACKATHON...
    category_name VARCHAR(120) NOT NULL,
    display_order INT NOT NULL DEFAULT 0,
    is_system     BOOLEAN NOT NULL DEFAULT TRUE,           -- TRUE = cố định, không xóa
    created_at    TIMESTAMP NOT NULL DEFAULT NOW()
);

-- Seed 7 nhóm cố định
INSERT INTO categories (code, category_name, display_order, is_system) VALUES
('INTERNSHIP',          'Thực tập',              1, TRUE),
('STARTUP_RECRUITMENT', 'Tuyển dụng Startup',    2, TRUE),
('INNOVATION_CONTEST',  'Cuộc thi Đổi mới Sáng tạo', 3, TRUE),
('HACKATHON',           'Hackathon',             4, TRUE),
('SCHOLARSHIP',         'Học bổng',              5, TRUE),
('INVESTMENT_FUND',     'Quỹ đầu tư',            6, TRUE),
('INCUBATOR',           'Chương trình ươm tạo',  7, TRUE);

-- ---------------------------------------------------------------------------
-- 5. DOMAINS  (3.3 / 4.3: lĩnh vực để lọc + notification preference)
-- ---------------------------------------------------------------------------
CREATE TABLE domains (
    domain_id   UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    domain_name VARCHAR(80) NOT NULL UNIQUE,              -- IT, Marketing, Finance...
    created_at  TIMESTAMP NOT NULL DEFAULT NOW()
);

-- ---------------------------------------------------------------------------
-- 6. OPPORTUNITIES  (Mục 4: bổ sung is_external, featured*, counters, slug, rejection)
-- ---------------------------------------------------------------------------
CREATE TABLE opportunities (
    opp_id           UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    org_id           UUID NOT NULL REFERENCES organizations(org_id),
    created_by       UUID NOT NULL REFERENCES users(user_id),
    category_id      UUID NOT NULL REFERENCES categories(category_id),
    title            VARCHAR(200) NOT NULL,
    slug             VARCHAR(220) NOT NULL UNIQUE,
    description      TEXT NOT NULL,                       -- đã SANITIZE (chống XSS)
    requirements     TEXT,
    benefits         TEXT,
    location         location_type NOT NULL DEFAULT 'TOAN_QUOC',
    work_type        work_type NOT NULL DEFAULT 'OFFLINE',
    apply_mode       apply_mode NOT NULL,                 -- INTERNAL | EXTERNAL
    external_link    VARCHAR(512),                        -- bắt buộc nếu EXTERNAL
    internal_form    JSONB,                               -- cấu hình form nộp nội bộ
    deadline         TIMESTAMP NOT NULL,
    status           opp_status NOT NULL DEFAULT 'DRAFT',
    rejection_reason TEXT,
    moderated_by     UUID REFERENCES users(user_id),
    moderated_at     TIMESTAMP,
    is_featured      BOOLEAN NOT NULL DEFAULT FALSE,      -- Mục 3: Featured
    featured_by      UUID REFERENCES users(user_id),
    featured_at      TIMESTAMP,
    featured_until   TIMESTAMP,
    view_count       INT NOT NULL DEFAULT 0,
    bookmark_count   INT NOT NULL DEFAULT 0,
    application_count INT NOT NULL DEFAULT 0,
    published_at     TIMESTAMP,
    created_at       TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at       TIMESTAMP NOT NULL DEFAULT NOW(),
    -- RÀNG BUỘC: EXTERNAL phải có link; INTERNAL không cần
    CONSTRAINT chk_external_link CHECK (
        (apply_mode = 'EXTERNAL' AND external_link IS NOT NULL AND external_link <> '')
        OR (apply_mode = 'INTERNAL')
    ),
    -- deadline phải ở tương lai khi tạo
    CONSTRAINT chk_deadline_future CHECK (deadline > created_at)
);
CREATE INDEX idx_opps_status_deadline ON opportunities(status, deadline);
CREATE INDEX idx_opps_category ON opportunities(category_id);
CREATE INDEX idx_opps_featured ON opportunities(is_featured, featured_until);
CREATE INDEX idx_opps_search ON opportunities USING gin (to_tsvector('simple', title || ' ' || description));  -- F02 full-text

-- Quan hệ N-N opp <-> domain
CREATE TABLE opportunity_domains (
    opp_id    UUID NOT NULL REFERENCES opportunities(opp_id) ON DELETE CASCADE,
    domain_id UUID NOT NULL REFERENCES domains(domain_id) ON DELETE CASCADE,
    PRIMARY KEY (opp_id, domain_id)
);

-- ---------------------------------------------------------------------------
-- 7. APPLICATIONS  (Mục 2: state machine + INTERVIEW + note + timestamps)
-- ---------------------------------------------------------------------------
CREATE TABLE applications (
    app_id         UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    opp_id         UUID NOT NULL REFERENCES opportunities(opp_id) ON DELETE CASCADE,
    student_id     UUID NOT NULL REFERENCES users(user_id),
    is_external    BOOLEAN NOT NULL DEFAULT FALSE,
    cv_file        VARCHAR(512),
    cover_letter   TEXT,
    status         app_status NOT NULL DEFAULT 'SUBMITTED',
    provider_note  TEXT,                                  -- phản hồi provider
    rejection_reason TEXT,
    applied_at     TIMESTAMP NOT NULL DEFAULT NOW(),
    reviewed_at    TIMESTAMP,
    interviewed_at TIMESTAMP,
    decided_at     TIMESTAMP,
    updated_at     TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_by     UUID REFERENCES users(user_id),
    UNIQUE (opp_id, student_id)                            -- S5: chặn nộp trùng
);
CREATE INDEX idx_apps_opp ON applications(opp_id);
CREATE INDEX idx_apps_student ON applications(student_id);

-- Lịch sử đổi trạng thái ứng tuyển (audit + gửi email)
CREATE TABLE application_status_history (
    id         UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    app_id     UUID NOT NULL REFERENCES applications(app_id) ON DELETE CASCADE,
    from_status app_status,
    to_status   app_status NOT NULL,
    changed_by  UUID REFERENCES users(user_id),
    note        TEXT,
    created_at  TIMESTAMP NOT NULL DEFAULT NOW()
);
CREATE INDEX idx_app_history ON application_status_history(app_id);

-- ---------------------------------------------------------------------------
-- 8. SAVED_OPPORTUNITIES  (F04.2: notify_before_hours 24-48)
-- ---------------------------------------------------------------------------
CREATE TABLE saved_opportunities (
    id                 UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    student_id         UUID NOT NULL REFERENCES users(user_id) ON DELETE CASCADE,
    opp_id             UUID NOT NULL REFERENCES opportunities(opp_id) ON DELETE CASCADE,
    notify_before_hours SMALLINT NOT NULL DEFAULT 48 CHECK (notify_before_hours BETWEEN 24 AND 48),
    saved_at           TIMESTAMP NOT NULL DEFAULT NOW(),
    UNIQUE (student_id, opp_id)
);
CREATE INDEX idx_saved_student ON saved_opportunities(student_id);

-- ---------------------------------------------------------------------------
-- 9. NOTIFICATIONS  (Mục 5: entity thiếu)
-- ---------------------------------------------------------------------------
CREATE TABLE notifications (
    notification_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id     UUID NOT NULL REFERENCES users(user_id) ON DELETE CASCADE,
    type        notif_type NOT NULL,
    channel     notif_channel NOT NULL,
    title       VARCHAR(255) NOT NULL,
    body        TEXT,
    ref_id      UUID,                                      -- opp_id / app_id
    is_read     BOOLEAN NOT NULL DEFAULT FALSE,
    sent_at     TIMESTAMP,
    created_at  TIMESTAMP NOT NULL DEFAULT NOW()
);
CREATE INDEX idx_notif_user_unread ON notifications(user_id, is_read);

-- ---------------------------------------------------------------------------
-- 10. NOTIFICATION_PREFERENCES  (Mục 5.3: tùy chọn + unsubscribe)
-- ---------------------------------------------------------------------------
CREATE TABLE notification_preferences (
    user_id    UUID NOT NULL REFERENCES users(user_id) ON DELETE CASCADE,
    type       notif_type NOT NULL,
    channel    notif_channel NOT NULL,
    enabled    BOOLEAN NOT NULL DEFAULT TRUE,
    frequency  notif_frequency NOT NULL DEFAULT 'INSTANT',
    categories JSONB DEFAULT '[]',                         -- mảng category_id (NEW_OPP)
    domains    JSONB DEFAULT '[]',                         -- mảng domain_id
    PRIMARY KEY (user_id, type, channel)
);

CREATE TABLE device_tokens (
    id         UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id    UUID NOT NULL REFERENCES users(user_id) ON DELETE CASCADE,
    token      VARCHAR(512) NOT NULL,
    platform   VARCHAR(20) NOT NULL,                      -- IOS/ANDROID/WEB
    updated_at TIMESTAMP NOT NULL DEFAULT NOW(),
    UNIQUE (user_id, token)
);

-- ---------------------------------------------------------------------------
-- 11. MODERATION & AUDIT LOGS  (Mục 5 bảo mật, A1.8)
-- ---------------------------------------------------------------------------
CREATE TABLE moderation_logs (
    id         UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    opp_id     UUID NOT NULL REFERENCES opportunities(opp_id) ON DELETE CASCADE,
    admin_id   UUID NOT NULL REFERENCES users(user_id),
    action     moderation_action NOT NULL,
    reason     TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT NOW()
);
CREATE INDEX idx_mod_logs_opp ON moderation_logs(opp_id);

CREATE TABLE audit_logs (
    id         UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    actor_id   UUID REFERENCES users(user_id),
    action     VARCHAR(120) NOT NULL,
    entity     VARCHAR(60) NOT NULL,
    entity_id  UUID,
    metadata   JSONB,
    created_at TIMESTAMP NOT NULL DEFAULT NOW()
);
CREATE INDEX idx_audit_actor ON audit_logs(actor_id, created_at);

-- ---------------------------------------------------------------------------
-- VIEW: trạng thái hiển thị dẫn xuất (Mục 3.1: tách status lưu vs hiển thị)
-- ---------------------------------------------------------------------------
CREATE VIEW opportunity_display_status AS
SELECT
    opp_id,
    status AS stored_status,
    CASE
        WHEN status = 'APPROVED' AND deadline < NOW() THEN 'EXPIRED'
        WHEN status = 'APPROVED' AND deadline < NOW() + INTERVAL '3 days' THEN 'CLOSING_SOON'
        WHEN status = 'APPROVED' THEN 'OPEN'
        WHEN status = 'HIDDEN' THEN 'HIDDEN'
        ELSE status::TEXT
    END AS display_status
FROM opportunities;
