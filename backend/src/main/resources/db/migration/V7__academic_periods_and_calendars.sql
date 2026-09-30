CREATE TABLE academic_period (
    period_id CHAR(36) NOT NULL,
    period_code VARCHAR(64) NOT NULL,
    period_kind VARCHAR(20) NOT NULL,
    academic_year SMALLINT NOT NULL,
    sequence_number SMALLINT NOT NULL,
    starts_on DATE NOT NULL,
    ends_on DATE NOT NULL,
    status VARCHAR(16) NOT NULL,
    approved_calendar_revision_id CHAR(36) NULL,
    created_by VARCHAR(180) NOT NULL,
    created_at TIMESTAMP(6) NOT NULL,
    approved_by VARCHAR(180) NULL,
    approved_at TIMESTAMP(6) NULL,
    opened_by VARCHAR(180) NULL,
    opened_at TIMESTAMP(6) NULL,
    closed_by VARCHAR(180) NULL,
    closed_at TIMESTAMP(6) NULL,
    cancelled_by VARCHAR(180) NULL,
    cancelled_at TIMESTAMP(6) NULL,
    PRIMARY KEY (period_id),
    CONSTRAINT uq_academic_period_code UNIQUE (period_code),
    CONSTRAINT uq_academic_period_kind_year_sequence UNIQUE (period_kind, academic_year, sequence_number),
    CONSTRAINT ck_academic_period_kind CHECK (period_kind IN ('REGULAR', 'INTERSEMESTRAL')),
    CONSTRAINT ck_academic_period_year CHECK (academic_year BETWEEN 1900 AND 9999),
    CONSTRAINT ck_academic_period_sequence CHECK (sequence_number >= 1),
    CONSTRAINT ck_academic_period_dates CHECK (ends_on >= starts_on),
    CONSTRAINT ck_academic_period_status CHECK (status IN ('DRAFT', 'APPROVED', 'OPEN', 'CLOSED', 'CANCELLED')),
    CONSTRAINT ck_academic_period_regular_sequence CHECK (period_kind <> 'REGULAR' OR sequence_number <= 2),
    CONSTRAINT ck_academic_period_metadata CHECK (
        (status = 'DRAFT' AND approved_calendar_revision_id IS NULL AND approved_by IS NULL AND approved_at IS NULL
            AND opened_by IS NULL AND opened_at IS NULL AND closed_by IS NULL AND closed_at IS NULL
            AND cancelled_by IS NULL AND cancelled_at IS NULL)
        OR (status = 'APPROVED' AND approved_calendar_revision_id IS NOT NULL AND approved_by IS NOT NULL
            AND approved_at IS NOT NULL AND opened_by IS NULL AND opened_at IS NULL AND closed_by IS NULL
            AND closed_at IS NULL AND cancelled_by IS NULL AND cancelled_at IS NULL)
        OR (status = 'OPEN' AND approved_calendar_revision_id IS NOT NULL AND approved_by IS NOT NULL
            AND approved_at IS NOT NULL AND opened_by IS NOT NULL AND opened_at IS NOT NULL
            AND closed_by IS NULL AND closed_at IS NULL AND cancelled_by IS NULL AND cancelled_at IS NULL)
        OR (status = 'CLOSED' AND approved_calendar_revision_id IS NOT NULL AND approved_by IS NOT NULL
            AND approved_at IS NOT NULL AND opened_by IS NOT NULL AND opened_at IS NOT NULL
            AND closed_by IS NOT NULL AND closed_at IS NOT NULL AND cancelled_by IS NULL AND cancelled_at IS NULL)
        OR (status = 'CANCELLED' AND cancelled_by IS NOT NULL AND cancelled_at IS NOT NULL
            AND opened_by IS NULL AND opened_at IS NULL AND closed_by IS NULL AND closed_at IS NULL)
    ),
    CONSTRAINT ck_academic_period_creator CHECK (CHAR_LENGTH(TRIM(created_by)) > 0),
    INDEX ix_academic_period_public (status, starts_on, period_kind, period_code),
    INDEX ix_academic_period_admin (status, period_kind, academic_year, starts_on, period_id)
);

CREATE TABLE academic_calendar_revision (
    calendar_revision_id CHAR(36) NOT NULL,
    period_id CHAR(36) NOT NULL,
    revision_number SMALLINT NOT NULL,
    official_reference VARCHAR(240) NULL,
    status VARCHAR(16) NOT NULL,
    created_by VARCHAR(180) NOT NULL,
    created_at TIMESTAMP(6) NOT NULL,
    published_by VARCHAR(180) NULL,
    published_at TIMESTAMP(6) NULL,
    PRIMARY KEY (calendar_revision_id),
    CONSTRAINT uq_academic_calendar_revision_number UNIQUE (period_id, revision_number),
    CONSTRAINT uq_academic_calendar_revision_id_period UNIQUE (calendar_revision_id, period_id),
    CONSTRAINT fk_academic_calendar_revision_period
        FOREIGN KEY (period_id) REFERENCES academic_period (period_id),
    CONSTRAINT ck_academic_calendar_revision_number CHECK (revision_number >= 1),
    CONSTRAINT ck_academic_calendar_revision_status CHECK (status IN ('DRAFT', 'PUBLISHED')),
    CONSTRAINT ck_academic_calendar_revision_reference
        CHECK (official_reference IS NULL OR CHAR_LENGTH(TRIM(official_reference)) > 0),
    CONSTRAINT ck_academic_calendar_revision_creator CHECK (CHAR_LENGTH(TRIM(created_by)) > 0),
    CONSTRAINT ck_academic_calendar_revision_publication CHECK (
        (status = 'DRAFT' AND published_by IS NULL AND published_at IS NULL)
        OR (status = 'PUBLISHED' AND official_reference IS NOT NULL
            AND published_by IS NOT NULL AND published_at IS NOT NULL)
    ),
    INDEX ix_academic_calendar_revision_history (period_id, revision_number)
);

ALTER TABLE academic_period
    ADD CONSTRAINT fk_academic_period_approved_calendar
        FOREIGN KEY (approved_calendar_revision_id, period_id)
        REFERENCES academic_calendar_revision (calendar_revision_id, period_id);

CREATE TABLE academic_calendar_activity (
    activity_id CHAR(36) NOT NULL,
    calendar_revision_id CHAR(36) NOT NULL,
    activity_key VARCHAR(64) NOT NULL,
    display_name VARCHAR(160) NOT NULL,
    starts_at TIMESTAMP(6) NOT NULL,
    ends_at TIMESTAMP(6) NOT NULL,
    organization_unit_id CHAR(36) NULL,
    site_id CHAR(36) NULL,
    PRIMARY KEY (activity_id),
    CONSTRAINT uq_academic_calendar_activity_key UNIQUE (calendar_revision_id, activity_key),
    CONSTRAINT fk_academic_calendar_activity_revision
        FOREIGN KEY (calendar_revision_id) REFERENCES academic_calendar_revision (calendar_revision_id),
    CONSTRAINT fk_academic_calendar_activity_unit
        FOREIGN KEY (organization_unit_id) REFERENCES academic_organization_unit (organization_unit_id),
    CONSTRAINT fk_academic_calendar_activity_site
        FOREIGN KEY (site_id) REFERENCES academic_site (site_id),
    CONSTRAINT ck_academic_calendar_activity_name CHECK (CHAR_LENGTH(TRIM(display_name)) > 0),
    CONSTRAINT ck_academic_calendar_activity_dates CHECK (ends_at >= starts_at),
    INDEX ix_academic_calendar_activity_revision_dates (calendar_revision_id, starts_at, ends_at),
    INDEX ix_academic_calendar_activity_scope (organization_unit_id, site_id, starts_at)
);

CREATE TABLE academic_period_audit_event (
    audit_event_id BIGINT NOT NULL AUTO_INCREMENT,
    period_id CHAR(36) NOT NULL,
    action_key VARCHAR(32) NOT NULL,
    actor_sub VARCHAR(180) NOT NULL,
    occurred_at TIMESTAMP(6) NOT NULL,
    reference VARCHAR(240) NULL,
    event_summary VARCHAR(240) NOT NULL,
    PRIMARY KEY (audit_event_id),
    CONSTRAINT fk_academic_period_audit_period
        FOREIGN KEY (period_id) REFERENCES academic_period (period_id),
    CONSTRAINT ck_academic_period_audit_action
        CHECK (action_key IN ('PERIOD_CREATED', 'CALENDAR_CREATED', 'CALENDAR_PUBLISHED',
            'PERIOD_APPROVED', 'PERIOD_OPENED', 'PERIOD_CLOSED', 'PERIOD_CANCELLED')),
    CONSTRAINT ck_academic_period_audit_actor CHECK (CHAR_LENGTH(TRIM(actor_sub)) > 0),
    CONSTRAINT ck_academic_period_audit_summary CHECK (CHAR_LENGTH(TRIM(event_summary)) > 0),
    INDEX ix_academic_period_audit_period_time (period_id, occurred_at)
);
