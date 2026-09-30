CREATE TABLE academic_program (
    program_id CHAR(36) NOT NULL,
    program_code VARCHAR(64) NOT NULL,
    academic_level VARCHAR(20) NOT NULL,
    study_modality VARCHAR(20) NOT NULL,
    campus_code VARCHAR(64) NOT NULL,
    created_at TIMESTAMP(6) NOT NULL,
    PRIMARY KEY (program_id),
    CONSTRAINT uq_academic_program_identity
        UNIQUE (program_code, academic_level, study_modality, campus_code),
    CONSTRAINT ck_academic_program_scope
        CHECK (academic_level = 'PREGRADO' AND study_modality = 'PRESENCIAL')
);

CREATE TABLE academic_program_revision (
    program_revision_id CHAR(36) NOT NULL,
    program_id CHAR(36) NOT NULL,
    content_fingerprint CHAR(64) NOT NULL,
    snies_code VARCHAR(32) NULL,
    program_name VARCHAR(240) NOT NULL,
    faculty VARCHAR(160) NOT NULL,
    campus_name VARCHAR(160) NOT NULL,
    created_at TIMESTAMP(6) NOT NULL,
    PRIMARY KEY (program_revision_id),
    CONSTRAINT uq_academic_program_revision_content
        UNIQUE (program_id, content_fingerprint),
    CONSTRAINT uq_academic_program_revision_id_program
        UNIQUE (program_revision_id, program_id),
    CONSTRAINT fk_academic_program_revision_program
        FOREIGN KEY (program_id) REFERENCES academic_program (program_id),
    CONSTRAINT ck_academic_program_revision_fingerprint
        CHECK (CHAR_LENGTH(content_fingerprint) = 64),
    CONSTRAINT ck_academic_program_revision_name
        CHECK (CHAR_LENGTH(TRIM(program_name)) > 0)
);

CREATE TABLE academic_subject (
    subject_id CHAR(36) NOT NULL,
    subject_code VARCHAR(64) NOT NULL,
    created_at TIMESTAMP(6) NOT NULL,
    PRIMARY KEY (subject_id),
    CONSTRAINT uq_academic_subject_code UNIQUE (subject_code)
);

CREATE TABLE academic_subject_revision (
    subject_revision_id CHAR(36) NOT NULL,
    subject_id CHAR(36) NOT NULL,
    content_fingerprint CHAR(64) NOT NULL,
    subject_name VARCHAR(240) NOT NULL,
    credits DECIMAL(5, 2) NOT NULL,
    created_at TIMESTAMP(6) NOT NULL,
    PRIMARY KEY (subject_revision_id),
    CONSTRAINT uq_academic_subject_revision_content
        UNIQUE (subject_id, content_fingerprint),
    CONSTRAINT uq_academic_subject_revision_id_subject
        UNIQUE (subject_revision_id, subject_id),
    CONSTRAINT fk_academic_subject_revision_subject
        FOREIGN KEY (subject_id) REFERENCES academic_subject (subject_id),
    CONSTRAINT ck_academic_subject_revision_fingerprint
        CHECK (CHAR_LENGTH(content_fingerprint) = 64),
    CONSTRAINT ck_academic_subject_revision_name
        CHECK (CHAR_LENGTH(TRIM(subject_name)) > 0),
    CONSTRAINT ck_academic_subject_revision_credits
        CHECK (credits > 0 AND credits <= 999.99)
);

CREATE TABLE academic_curriculum (
    curriculum_id CHAR(36) NOT NULL,
    program_id CHAR(36) NOT NULL,
    program_revision_id CHAR(36) NOT NULL,
    curriculum_version VARCHAR(80) NOT NULL,
    cohort_from CHAR(6) NOT NULL,
    cohort_through CHAR(6) NULL,
    approval_reference VARCHAR(240) NOT NULL,
    status VARCHAR(16) NOT NULL,
    source_sha256 CHAR(64) NOT NULL,
    created_by VARCHAR(180) NOT NULL,
    created_at TIMESTAMP(6) NOT NULL,
    published_by VARCHAR(180) NULL,
    published_at TIMESTAMP(6) NULL,
    PRIMARY KEY (curriculum_id),
    CONSTRAINT uq_academic_curriculum_program_version
        UNIQUE (program_id, curriculum_version),
    CONSTRAINT fk_academic_curriculum_program
        FOREIGN KEY (program_id) REFERENCES academic_program (program_id),
    CONSTRAINT fk_academic_curriculum_program_revision
        FOREIGN KEY (program_revision_id, program_id)
        REFERENCES academic_program_revision (program_revision_id, program_id),
    CONSTRAINT ck_academic_curriculum_status
        CHECK (status IN ('DRAFT', 'PUBLISHED')),
    CONSTRAINT ck_academic_curriculum_cohort_from
        CHECK (CHAR_LENGTH(cohort_from) = 6),
    CONSTRAINT ck_academic_curriculum_cohort_through
        CHECK (cohort_through IS NULL OR (CHAR_LENGTH(cohort_through) = 6 AND cohort_through >= cohort_from)),
    CONSTRAINT ck_academic_curriculum_version
        CHECK (CHAR_LENGTH(TRIM(curriculum_version)) > 0),
    CONSTRAINT ck_academic_curriculum_approval
        CHECK (CHAR_LENGTH(TRIM(approval_reference)) > 0),
    CONSTRAINT ck_academic_curriculum_source_hash
        CHECK (CHAR_LENGTH(source_sha256) = 64),
    CONSTRAINT ck_academic_curriculum_publication_state
        CHECK ((status = 'DRAFT' AND published_by IS NULL AND published_at IS NULL)
            OR (status = 'PUBLISHED' AND published_by IS NOT NULL AND published_at IS NOT NULL)),
    INDEX ix_academic_curriculum_public (status, program_id, cohort_from, cohort_through),
    INDEX ix_academic_curriculum_drafts (status, created_at)
);

CREATE TABLE academic_curriculum_entry (
    entry_id BIGINT NOT NULL AUTO_INCREMENT,
    curriculum_id CHAR(36) NOT NULL,
    subject_id CHAR(36) NOT NULL,
    subject_revision_id CHAR(36) NOT NULL,
    semester SMALLINT NOT NULL,
    formation_space VARCHAR(120) NOT NULL,
    component VARCHAR(120) NOT NULL,
    choice_group VARCHAR(100) NULL,
    row_order INT NOT NULL,
    PRIMARY KEY (entry_id),
    CONSTRAINT uq_academic_curriculum_entry_subject
        UNIQUE (curriculum_id, subject_id),
    CONSTRAINT uq_academic_curriculum_entry_order
        UNIQUE (curriculum_id, row_order),
    CONSTRAINT fk_academic_curriculum_entry_curriculum
        FOREIGN KEY (curriculum_id) REFERENCES academic_curriculum (curriculum_id),
    CONSTRAINT fk_academic_curriculum_entry_subject_revision
        FOREIGN KEY (subject_revision_id, subject_id)
        REFERENCES academic_subject_revision (subject_revision_id, subject_id),
    CONSTRAINT ck_academic_curriculum_entry_semester
        CHECK (semester BETWEEN 1 AND 32767),
    CONSTRAINT ck_academic_curriculum_entry_order
        CHECK (row_order BETWEEN 1 AND 10000),
    CONSTRAINT ck_academic_curriculum_entry_space
        CHECK (CHAR_LENGTH(TRIM(formation_space)) > 0),
    CONSTRAINT ck_academic_curriculum_entry_component
        CHECK (CHAR_LENGTH(TRIM(component)) > 0),
    INDEX ix_academic_curriculum_entry_order_lookup (curriculum_id, semester, row_order)
);

CREATE TABLE academic_catalog_audit_event (
    audit_event_id BIGINT NOT NULL AUTO_INCREMENT,
    curriculum_id CHAR(36) NOT NULL,
    actor_sub VARCHAR(180) NOT NULL,
    action_key VARCHAR(32) NOT NULL,
    source_sha256 CHAR(64) NOT NULL,
    occurred_at TIMESTAMP(6) NOT NULL,
    event_summary VARCHAR(240) NOT NULL,
    PRIMARY KEY (audit_event_id),
    CONSTRAINT fk_academic_catalog_audit_curriculum
        FOREIGN KEY (curriculum_id) REFERENCES academic_curriculum (curriculum_id),
    CONSTRAINT ck_academic_catalog_audit_action
        CHECK (action_key IN ('CURRICULUM_IMPORTED', 'CURRICULUM_PUBLISHED')),
    CONSTRAINT ck_academic_catalog_audit_source_hash
        CHECK (CHAR_LENGTH(source_sha256) = 64),
    CONSTRAINT ck_academic_catalog_audit_actor
        CHECK (CHAR_LENGTH(TRIM(actor_sub)) > 0),
    INDEX ix_academic_catalog_audit_curriculum_time (curriculum_id, occurred_at)
);
