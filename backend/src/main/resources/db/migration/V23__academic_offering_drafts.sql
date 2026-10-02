CREATE TABLE academic_offering_draft (
    offering_id CHAR(36) NOT NULL,
    period_id CHAR(36) NOT NULL,
    curriculum_id CHAR(36) NOT NULL,
    subject_id CHAR(36) NOT NULL,
    section_code VARCHAR(24) NOT NULL,
    starts_on DATE NOT NULL,
    ends_on DATE NOT NULL,
    proposed_capacity INT NOT NULL,
    source_reference VARCHAR(240) NOT NULL,
    version INT NOT NULL,
    created_by VARCHAR(255) NOT NULL,
    created_at TIMESTAMP(6) NOT NULL,
    updated_by VARCHAR(255) NOT NULL,
    updated_at TIMESTAMP(6) NOT NULL,
    PRIMARY KEY (offering_id),
    CONSTRAINT uq_academic_offering_draft_section
        UNIQUE (period_id, curriculum_id, subject_id, section_code),
    CONSTRAINT fk_academic_offering_draft_period
        FOREIGN KEY (period_id) REFERENCES academic_period (period_id),
    CONSTRAINT fk_academic_offering_draft_curriculum_entry
        FOREIGN KEY (curriculum_id, subject_id)
        REFERENCES academic_curriculum_entry (curriculum_id, subject_id),
    CONSTRAINT ck_academic_offering_draft_dates CHECK (ends_on >= starts_on),
    CONSTRAINT ck_academic_offering_draft_capacity CHECK (proposed_capacity > 0),
    CONSTRAINT ck_academic_offering_draft_version CHECK (version >= 1),
    CONSTRAINT ck_academic_offering_draft_section_code CHECK (CHAR_LENGTH(TRIM(section_code)) > 0),
    CONSTRAINT ck_academic_offering_draft_source_reference CHECK (CHAR_LENGTH(TRIM(source_reference)) > 0),
    CONSTRAINT ck_academic_offering_draft_actors CHECK (
        CHAR_LENGTH(TRIM(created_by)) > 0 AND CHAR_LENGTH(TRIM(updated_by)) > 0
    ),
    INDEX ix_academic_offering_draft_period_order (period_id, created_at, offering_id)
);

CREATE TABLE academic_offering_draft_audit_event (
    audit_event_id BIGINT NOT NULL AUTO_INCREMENT,
    offering_id CHAR(36) NOT NULL,
    action_key VARCHAR(32) NOT NULL,
    actor_sub VARCHAR(255) NOT NULL,
    occurred_at TIMESTAMP(6) NOT NULL,
    source_reference VARCHAR(240) NOT NULL,
    before_section_code VARCHAR(24) NULL,
    before_starts_on DATE NULL,
    before_ends_on DATE NULL,
    before_proposed_capacity INT NULL,
    before_version INT NULL,
    after_section_code VARCHAR(24) NOT NULL,
    after_starts_on DATE NOT NULL,
    after_ends_on DATE NOT NULL,
    after_proposed_capacity INT NOT NULL,
    after_version INT NOT NULL,
    PRIMARY KEY (audit_event_id),
    CONSTRAINT fk_academic_offering_audit_draft
        FOREIGN KEY (offering_id) REFERENCES academic_offering_draft (offering_id),
    CONSTRAINT ck_academic_offering_audit_action
        CHECK (action_key IN ('OFFERING_DRAFT_CREATED', 'OFFERING_DRAFT_UPDATED')),
    CONSTRAINT ck_academic_offering_audit_actor CHECK (CHAR_LENGTH(TRIM(actor_sub)) > 0),
    CONSTRAINT ck_academic_offering_audit_reference CHECK (CHAR_LENGTH(TRIM(source_reference)) > 0),
    CONSTRAINT ck_academic_offering_audit_snapshot CHECK (
        (action_key = 'OFFERING_DRAFT_CREATED'
            AND before_section_code IS NULL AND before_starts_on IS NULL AND before_ends_on IS NULL
            AND before_proposed_capacity IS NULL AND before_version IS NULL AND after_version = 1)
        OR (action_key = 'OFFERING_DRAFT_UPDATED'
            AND before_section_code IS NOT NULL AND before_starts_on IS NOT NULL AND before_ends_on IS NOT NULL
            AND before_proposed_capacity IS NOT NULL AND before_version IS NOT NULL
            AND after_version = before_version + 1)
    ),
    INDEX ix_academic_offering_audit_history (offering_id, audit_event_id)
);
