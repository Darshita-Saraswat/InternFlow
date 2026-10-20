CREATE TABLE opportunity_catalogue (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    company VARCHAR(200) NOT NULL,
    role_title VARCHAR(200) NOT NULL,
    role_type VARCHAR(40) NOT NULL,
    url TEXT,
    source VARCHAR(40),
    location VARCHAR(200),
    work_mode VARCHAR(20),
    description TEXT,
    deadline DATE,
    eligible_graduation_from SMALLINT,
    eligible_graduation_to SMALLINT,
    required_skills TEXT[] NOT NULL DEFAULT ARRAY[]::TEXT[],
    compensation_min NUMERIC(12, 2),
    compensation_max NUMERIC(12, 2),
    compensation_currency CHAR(3),
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT opportunity_catalogue_company_nonempty CHECK (LENGTH(BTRIM(company)) > 0),
    CONSTRAINT opportunity_catalogue_role_title_nonempty CHECK (LENGTH(BTRIM(role_title)) > 0),
    CONSTRAINT opportunity_catalogue_role_type_valid CHECK (
        role_type IN ('internship', 'graduate_role', 'analyst_role', 'early_career_other')
    ),
    CONSTRAINT opportunity_catalogue_source_valid CHECK (
        source IS NULL OR source IN ('careers_page', 'campus_placement', 'referral', 'job_board', 'event', 'other')
    ),
    CONSTRAINT opportunity_catalogue_work_mode_valid CHECK (
        work_mode IS NULL OR work_mode IN ('in_person', 'hybrid', 'remote')
    ),
    CONSTRAINT opportunity_catalogue_graduation_range_valid CHECK (
        eligible_graduation_from IS NULL
        OR eligible_graduation_to IS NULL
        OR eligible_graduation_from <= eligible_graduation_to
    ),
    CONSTRAINT opportunity_catalogue_compensation_nonnegative CHECK (
        (compensation_min IS NULL OR compensation_min >= 0)
        AND (compensation_max IS NULL OR compensation_max >= 0)
    ),
    CONSTRAINT opportunity_catalogue_compensation_range_valid CHECK (
        compensation_min IS NULL OR compensation_max IS NULL OR compensation_min <= compensation_max
    ),
    CONSTRAINT opportunity_catalogue_currency_valid CHECK (
        compensation_currency IS NULL OR compensation_currency ~ '^[A-Z]{3}$'
    )
);

CREATE INDEX opportunity_catalogue_role_type_idx ON opportunity_catalogue (role_type);
CREATE INDEX opportunity_catalogue_location_idx ON opportunity_catalogue (location);
CREATE INDEX opportunity_catalogue_work_mode_idx ON opportunity_catalogue (work_mode);
CREATE INDEX opportunity_catalogue_deadline_idx ON opportunity_catalogue (deadline);
