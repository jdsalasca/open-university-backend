ALTER TABLE institutional_identity
    ADD CONSTRAINT uq_institutional_identity_identity_user UNIQUE (identity_id, user_id);

CREATE TABLE admissions_call (
    call_id CHAR(36) NOT NULL,
    call_key VARCHAR(64) NOT NULL,
    current_published_revision_id CHAR(36) NULL,
    created_by_user_id CHAR(36) NOT NULL,
    created_by_identity_id CHAR(36) NOT NULL,
    created_at TIMESTAMP(6) NOT NULL,
    PRIMARY KEY (call_id),
    CONSTRAINT uq_admissions_call_key UNIQUE (call_key),
    CONSTRAINT fk_admissions_call_creator
        FOREIGN KEY (created_by_identity_id, created_by_user_id)
        REFERENCES institutional_identity (identity_id, user_id),
    CONSTRAINT fk_admissions_call_creator_user
        FOREIGN KEY (created_by_user_id) REFERENCES university_user (user_id),
    CONSTRAINT ck_admissions_call_key CHECK (CHAR_LENGTH(TRIM(call_key)) BETWEEN 1 AND 64),
    INDEX ix_admissions_call_public (current_published_revision_id, call_key)
);

CREATE TABLE admissions_call_revision (
    revision_id CHAR(36) NOT NULL,
    call_id CHAR(36) NOT NULL,
    revision_number INT NOT NULL,
    draft_version INT NOT NULL,
    status VARCHAR(16) NOT NULL,
    title VARCHAR(160) NOT NULL,
    call_name VARCHAR(160) NOT NULL,
    updated_at DATE NOT NULL,
    checked_at DATE NOT NULL,
    source_label VARCHAR(120) NOT NULL,
    source_url VARCHAR(500) NOT NULL,
    confirmation_source_label VARCHAR(120) NOT NULL,
    confirmation_source_url VARCHAR(500) NOT NULL,
    created_by_user_id CHAR(36) NOT NULL,
    created_by_identity_id CHAR(36) NOT NULL,
    created_at TIMESTAMP(6) NOT NULL,
    published_by_user_id CHAR(36) NULL,
    published_by_identity_id CHAR(36) NULL,
    published_at TIMESTAMP(6) NULL,
    official_reference VARCHAR(240) NULL,
    PRIMARY KEY (revision_id),
    CONSTRAINT uq_admissions_call_revision_number UNIQUE (call_id, revision_number),
    CONSTRAINT uq_admissions_call_revision_id UNIQUE (call_id, revision_id),
    CONSTRAINT fk_admissions_revision_call
        FOREIGN KEY (call_id) REFERENCES admissions_call (call_id),
    CONSTRAINT fk_admissions_revision_creator
        FOREIGN KEY (created_by_identity_id, created_by_user_id)
        REFERENCES institutional_identity (identity_id, user_id),
    CONSTRAINT fk_admissions_revision_creator_user
        FOREIGN KEY (created_by_user_id) REFERENCES university_user (user_id),
    CONSTRAINT fk_admissions_revision_publisher
        FOREIGN KEY (published_by_identity_id, published_by_user_id)
        REFERENCES institutional_identity (identity_id, user_id),
    CONSTRAINT fk_admissions_revision_publisher_user
        FOREIGN KEY (published_by_user_id) REFERENCES university_user (user_id),
    CONSTRAINT ck_admissions_revision_number CHECK (revision_number >= 1),
    CONSTRAINT ck_admissions_revision_draft_version CHECK (draft_version >= 1),
    CONSTRAINT ck_admissions_revision_status CHECK (status IN ('DRAFT', 'PUBLISHED')),
    CONSTRAINT ck_admissions_revision_text CHECK (
        CHAR_LENGTH(TRIM(title)) > 0 AND CHAR_LENGTH(TRIM(call_name)) > 0
        AND CHAR_LENGTH(TRIM(source_label)) > 0 AND CHAR_LENGTH(TRIM(source_url)) > 0
        AND CHAR_LENGTH(TRIM(confirmation_source_label)) > 0
        AND CHAR_LENGTH(TRIM(confirmation_source_url)) > 0
    ),
    CONSTRAINT ck_admissions_revision_dates CHECK (checked_at >= updated_at),
    CONSTRAINT ck_admissions_revision_publication CHECK (
        (status = 'DRAFT' AND published_by_user_id IS NULL AND published_by_identity_id IS NULL
            AND published_at IS NULL AND official_reference IS NULL)
        OR (status = 'PUBLISHED' AND published_by_user_id IS NOT NULL AND published_by_identity_id IS NOT NULL
            AND published_at IS NOT NULL AND official_reference IS NOT NULL
            AND CHAR_LENGTH(TRIM(official_reference)) BETWEEN 1 AND 240)
    ),
    INDEX ix_admissions_revision_history (call_id, revision_number),
    INDEX ix_admissions_revision_publication (status, published_at)
);

ALTER TABLE admissions_call
    ADD CONSTRAINT fk_admissions_call_published_revision
        FOREIGN KEY (call_id, current_published_revision_id)
        REFERENCES admissions_call_revision (call_id, revision_id);

CREATE TABLE admissions_call_milestone (
    revision_id CHAR(36) NOT NULL,
    milestone_key VARCHAR(64) NOT NULL,
    milestone_kind VARCHAR(24) NOT NULL,
    starts_on DATE NOT NULL,
    ends_on DATE NOT NULL,
    title VARCHAR(160) NOT NULL,
    description VARCHAR(500) NOT NULL,
    PRIMARY KEY (revision_id, milestone_key),
    CONSTRAINT fk_admissions_milestone_revision
        FOREIGN KEY (revision_id) REFERENCES admissions_call_revision (revision_id),
    CONSTRAINT ck_admissions_milestone_kind CHECK (milestone_kind IN ('APPLICATION', 'SELECTION', 'ENROLLMENT')),
    CONSTRAINT ck_admissions_milestone_dates CHECK (ends_on >= starts_on),
    CONSTRAINT ck_admissions_milestone_text CHECK (
        CHAR_LENGTH(TRIM(milestone_key)) BETWEEN 1 AND 64
        AND CHAR_LENGTH(TRIM(title)) BETWEEN 1 AND 160
        AND CHAR_LENGTH(TRIM(description)) BETWEEN 1 AND 500
    ),
    INDEX ix_admissions_milestone_order (revision_id, starts_on, milestone_key)
);

CREATE TABLE admissions_call_audit_event (
    audit_event_id BIGINT NOT NULL AUTO_INCREMENT,
    call_id CHAR(36) NOT NULL,
    revision_id CHAR(36) NOT NULL,
    action_key VARCHAR(32) NOT NULL,
    actor_user_id CHAR(36) NOT NULL,
    actor_identity_id CHAR(36) NOT NULL,
    occurred_at TIMESTAMP(6) NOT NULL,
    official_reference VARCHAR(240) NULL,
    event_summary VARCHAR(240) NOT NULL,
    PRIMARY KEY (audit_event_id),
    CONSTRAINT fk_admissions_audit_call
        FOREIGN KEY (call_id) REFERENCES admissions_call (call_id),
    CONSTRAINT fk_admissions_audit_revision
        FOREIGN KEY (call_id, revision_id) REFERENCES admissions_call_revision (call_id, revision_id),
    CONSTRAINT fk_admissions_audit_actor
        FOREIGN KEY (actor_identity_id, actor_user_id)
        REFERENCES institutional_identity (identity_id, user_id),
    CONSTRAINT ck_admissions_audit_action CHECK (
        action_key IN ('CALL_CREATED', 'REVISION_CREATED', 'REVISION_UPDATED', 'REVISION_PUBLISHED')
    ),
    CONSTRAINT ck_admissions_audit_summary CHECK (CHAR_LENGTH(TRIM(event_summary)) BETWEEN 1 AND 240),
    INDEX ix_admissions_audit_call_time (call_id, occurred_at, audit_event_id),
    INDEX ix_admissions_audit_actor_time (actor_user_id, occurred_at)
);
