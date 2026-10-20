CREATE TABLE users (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    email VARCHAR(320) NOT NULL,
    display_name VARCHAR(120) NOT NULL,
    password_hash VARCHAR(100) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT users_email_normalized CHECK (email = LOWER(BTRIM(email))),
    CONSTRAINT users_email_nonempty CHECK (LENGTH(email) > 0),
    CONSTRAINT users_display_name_nonempty CHECK (LENGTH(BTRIM(display_name)) > 0),
    CONSTRAINT users_password_hash_nonempty CHECK (LENGTH(password_hash) > 0),
    CONSTRAINT users_id_unique UNIQUE (id)
);

CREATE UNIQUE INDEX users_email_unique_idx ON users (email);

CREATE TABLE student_profiles (
    user_id UUID PRIMARY KEY,
    university VARCHAR(200),
    degree VARCHAR(200),
    graduation_date DATE,
    study_year SMALLINT,
    target_role_categories TEXT[] NOT NULL DEFAULT ARRAY[]::TEXT[],
    skills TEXT[] NOT NULL DEFAULT ARRAY[]::TEXT[],
    preferred_locations TEXT[] NOT NULL DEFAULT ARRAY[]::TEXT[],
    work_modes TEXT[] NOT NULL DEFAULT ARRAY[]::TEXT[],
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT student_profiles_user_fk FOREIGN KEY (user_id)
        REFERENCES users (id) ON DELETE CASCADE,
    CONSTRAINT student_profiles_study_year_range CHECK (study_year IS NULL OR study_year BETWEEN 1 AND 10),
    CONSTRAINT student_profiles_work_modes_valid CHECK (
        work_modes <@ ARRAY['in_person', 'hybrid', 'remote']::TEXT[]
    )
);

CREATE TABLE opportunities (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL,
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
    CONSTRAINT opportunities_user_fk FOREIGN KEY (user_id)
        REFERENCES users (id) ON DELETE CASCADE,
    CONSTRAINT opportunities_user_id_unique UNIQUE (user_id, id),
    CONSTRAINT opportunities_company_nonempty CHECK (LENGTH(BTRIM(company)) > 0),
    CONSTRAINT opportunities_role_title_nonempty CHECK (LENGTH(BTRIM(role_title)) > 0),
    CONSTRAINT opportunities_role_type_valid CHECK (
        role_type IN ('internship', 'graduate_role', 'analyst_role', 'early_career_other')
    ),
    CONSTRAINT opportunities_source_valid CHECK (
        source IS NULL OR source IN ('careers_page', 'campus_placement', 'referral', 'job_board', 'event', 'other')
    ),
    CONSTRAINT opportunities_work_mode_valid CHECK (
        work_mode IS NULL OR work_mode IN ('in_person', 'hybrid', 'remote')
    ),
    CONSTRAINT opportunities_graduation_range_valid CHECK (
        eligible_graduation_from IS NULL
        OR eligible_graduation_to IS NULL
        OR eligible_graduation_from <= eligible_graduation_to
    ),
    CONSTRAINT opportunities_compensation_nonnegative CHECK (
        (compensation_min IS NULL OR compensation_min >= 0)
        AND (compensation_max IS NULL OR compensation_max >= 0)
    ),
    CONSTRAINT opportunities_compensation_range_valid CHECK (
        compensation_min IS NULL OR compensation_max IS NULL OR compensation_min <= compensation_max
    )
);

CREATE INDEX opportunities_user_deadline_idx ON opportunities (user_id, deadline);

CREATE TABLE applications (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL,
    opportunity_id UUID,
    company VARCHAR(200) NOT NULL,
    role_title VARCHAR(200) NOT NULL,
    role_type VARCHAR(40) NOT NULL,
    url TEXT,
    source VARCHAR(40),
    location VARCHAR(200),
    work_mode VARCHAR(20),
    saved_on DATE NOT NULL DEFAULT CURRENT_DATE,
    deadline DATE,
    applied_on DATE,
    follow_up_on DATE,
    assessment_at TIMESTAMPTZ,
    interview_at TIMESTAMPTZ,
    status VARCHAR(30) NOT NULL DEFAULT 'saved',
    priority VARCHAR(10) NOT NULL DEFAULT 'normal',
    next_action VARCHAR(500),
    compensation_min NUMERIC(12, 2),
    compensation_max NUMERIC(12, 2),
    compensation_currency CHAR(3),
    required_skills TEXT[] NOT NULL DEFAULT ARRAY[]::TEXT[],
    notes TEXT,
    document_reference TEXT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT applications_user_fk FOREIGN KEY (user_id)
        REFERENCES users (id) ON DELETE CASCADE,
    CONSTRAINT applications_opportunity_user_fk FOREIGN KEY (user_id, opportunity_id)
        REFERENCES opportunities (user_id, id) ON DELETE SET NULL (opportunity_id),
    CONSTRAINT applications_user_id_unique UNIQUE (user_id, id),
    CONSTRAINT applications_company_nonempty CHECK (LENGTH(BTRIM(company)) > 0),
    CONSTRAINT applications_role_title_nonempty CHECK (LENGTH(BTRIM(role_title)) > 0),
    CONSTRAINT applications_role_type_valid CHECK (
        role_type IN ('internship', 'graduate_role', 'analyst_role', 'early_career_other')
    ),
    CONSTRAINT applications_source_valid CHECK (
        source IS NULL OR source IN ('careers_page', 'campus_placement', 'referral', 'job_board', 'event', 'other')
    ),
    CONSTRAINT applications_work_mode_valid CHECK (
        work_mode IS NULL OR work_mode IN ('in_person', 'hybrid', 'remote')
    ),
    CONSTRAINT applications_status_valid CHECK (
        status IN ('saved', 'preparing', 'applied', 'online_assessment', 'interview', 'offer', 'rejected', 'withdrawn')
    ),
    CONSTRAINT applications_priority_valid CHECK (priority IN ('low', 'normal', 'high')),
    CONSTRAINT applications_compensation_nonnegative CHECK (
        (compensation_min IS NULL OR compensation_min >= 0)
        AND (compensation_max IS NULL OR compensation_max >= 0)
    ),
    CONSTRAINT applications_compensation_range_valid CHECK (
        compensation_min IS NULL OR compensation_max IS NULL OR compensation_min <= compensation_max
    )
);

CREATE INDEX applications_user_status_idx ON applications (user_id, status);
CREATE INDEX applications_user_deadline_idx ON applications (user_id, deadline)
    WHERE deadline IS NOT NULL;
CREATE INDEX applications_user_follow_up_idx ON applications (user_id, follow_up_on)
    WHERE follow_up_on IS NOT NULL;

CREATE TABLE application_events (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL,
    application_id UUID NOT NULL,
    event_type VARCHAR(30) NOT NULL,
    status_before VARCHAR(30),
    status_after VARCHAR(30),
    description TEXT,
    occurred_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT application_events_application_user_fk FOREIGN KEY (user_id, application_id)
        REFERENCES applications (user_id, id) ON DELETE CASCADE,
    CONSTRAINT application_events_event_type_valid CHECK (
        event_type IN (
            'created', 'status_changed', 'note_added', 'follow_up_completed',
            'assessment_scheduled', 'interview_scheduled', 'updated'
        )
    ),
    CONSTRAINT application_events_status_before_valid CHECK (
        status_before IS NULL OR status_before IN (
            'saved', 'preparing', 'applied', 'online_assessment', 'interview', 'offer', 'rejected', 'withdrawn'
        )
    ),
    CONSTRAINT application_events_status_after_valid CHECK (
        status_after IS NULL OR status_after IN (
            'saved', 'preparing', 'applied', 'online_assessment', 'interview', 'offer', 'rejected', 'withdrawn'
        )
    )
);

CREATE INDEX application_events_user_application_time_idx
    ON application_events (user_id, application_id, occurred_at DESC);

CREATE TABLE tasks (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL,
    application_id UUID,
    title VARCHAR(200) NOT NULL,
    description TEXT,
    priority VARCHAR(10) NOT NULL DEFAULT 'normal',
    category VARCHAR(30) NOT NULL DEFAULT 'other',
    status VARCHAR(20) NOT NULL DEFAULT 'todo',
    due_at TIMESTAMPTZ,
    completed_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT tasks_user_fk FOREIGN KEY (user_id)
        REFERENCES users (id) ON DELETE CASCADE,
    CONSTRAINT tasks_application_user_fk FOREIGN KEY (user_id, application_id)
        REFERENCES applications (user_id, id) ON DELETE CASCADE,
    CONSTRAINT tasks_title_nonempty CHECK (LENGTH(BTRIM(title)) > 0),
    CONSTRAINT tasks_priority_valid CHECK (priority IN ('low', 'normal', 'high')),
    CONSTRAINT tasks_category_valid CHECK (
        category IN (
            'resume', 'research', 'online_assessment', 'dsa_practice',
            'interview_preparation', 'networking', 'follow_up', 'documents', 'other'
        )
    ),
    CONSTRAINT tasks_status_valid CHECK (status IN ('todo', 'in_progress', 'completed')),
    CONSTRAINT tasks_completion_consistent CHECK (
        (status = 'completed') = (completed_at IS NOT NULL)
    )
);

CREATE INDEX tasks_user_status_due_idx ON tasks (user_id, status, due_at);
CREATE INDEX tasks_user_application_idx ON tasks (user_id, application_id)
    WHERE application_id IS NOT NULL;

CREATE TABLE notification_preferences (
    user_id UUID PRIMARY KEY,
    in_app_alerts_enabled BOOLEAN NOT NULL DEFAULT TRUE,
    deadline_reminder_days SMALLINT NOT NULL DEFAULT 3,
    follow_up_reminders_enabled BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT notification_preferences_user_fk FOREIGN KEY (user_id)
        REFERENCES users (id) ON DELETE CASCADE,
    CONSTRAINT notification_preferences_reminder_days_range CHECK (
        deadline_reminder_days BETWEEN 0 AND 30
    )
);
