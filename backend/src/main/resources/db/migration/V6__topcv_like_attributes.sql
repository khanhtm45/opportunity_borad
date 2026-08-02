-- Thuộc tính tin đăng / tổ chức kiểu TopCV (idempotent)
DO $$ BEGIN
    CREATE TYPE job_level AS ENUM ('INTERN', 'STAFF', 'TEAM_LEAD', 'MANAGER', 'DIRECTOR', 'OTHER');
EXCEPTION WHEN duplicate_object THEN NULL;
END $$;

DO $$ BEGIN
    CREATE TYPE experience_level AS ENUM ('NONE', 'UNDER_ONE_YEAR', 'ONE_TO_TWO', 'TWO_TO_THREE', 'THREE_TO_FIVE', 'FIVE_PLUS');
EXCEPTION WHEN duplicate_object THEN NULL;
END $$;

DO $$ BEGIN
    CREATE TYPE education_level AS ENUM ('NONE', 'HIGH_SCHOOL', 'INTERMEDIATE', 'COLLEGE', 'UNIVERSITY', 'POSTGRAD');
EXCEPTION WHEN duplicate_object THEN NULL;
END $$;

DO $$ BEGIN
    CREATE TYPE employment_type AS ENUM ('FULL_TIME', 'PART_TIME', 'CONTRACT', 'FREELANCE', 'OTHER');
EXCEPTION WHEN duplicate_object THEN NULL;
END $$;

DO $$ BEGIN
    CREATE TYPE company_size AS ENUM ('SIZE_1_10', 'SIZE_11_50', 'SIZE_51_200', 'SIZE_201_500', 'SIZE_500_PLUS', 'UNKNOWN');
EXCEPTION WHEN duplicate_object THEN NULL;
END $$;

ALTER TABLE opportunities
    ADD COLUMN IF NOT EXISTS job_level job_level,
    ADD COLUMN IF NOT EXISTS experience_level experience_level,
    ADD COLUMN IF NOT EXISTS education_level education_level,
    ADD COLUMN IF NOT EXISTS headcount INT,
    ADD COLUMN IF NOT EXISTS employment_type employment_type,
    ADD COLUMN IF NOT EXISTS salary_min BIGINT,
    ADD COLUMN IF NOT EXISTS salary_max BIGINT,
    ADD COLUMN IF NOT EXISTS salary_currency VARCHAR(10) DEFAULT 'VND',
    ADD COLUMN IF NOT EXISTS salary_negotiable BOOLEAN NOT NULL DEFAULT FALSE,
    ADD COLUMN IF NOT EXISTS address_detail VARCHAR(500),
    ADD COLUMN IF NOT EXISTS working_schedule TEXT,
    ADD COLUMN IF NOT EXISTS skills TEXT;

ALTER TABLE organizations
    ADD COLUMN IF NOT EXISTS tax_code VARCHAR(40),
    ADD COLUMN IF NOT EXISTS address VARCHAR(500),
    ADD COLUMN IF NOT EXISTS industry VARCHAR(200),
    ADD COLUMN IF NOT EXISTS company_size company_size;

CREATE INDEX IF NOT EXISTS idx_opps_job_level ON opportunities(job_level);
CREATE INDEX IF NOT EXISTS idx_opps_employment_type ON opportunities(employment_type);
CREATE INDEX IF NOT EXISTS idx_opps_experience_level ON opportunities(experience_level);
