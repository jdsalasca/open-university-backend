CREATE TABLE institutional_identity (
    identity_id CHAR(36) NOT NULL,
    issuer VARCHAR(2048) NOT NULL,
    subject VARBINARY(255) NOT NULL,
    issuer_sha256 BINARY(32) NOT NULL,
    first_seen_at TIMESTAMP(6) NOT NULL,
    PRIMARY KEY (identity_id),
    CONSTRAINT uq_institutional_identity_issuer_subject UNIQUE (issuer_sha256, subject),
    CONSTRAINT ck_institutional_identity_issuer CHECK (CHAR_LENGTH(TRIM(issuer)) > 0),
    CONSTRAINT ck_institutional_identity_subject CHECK (OCTET_LENGTH(subject) BETWEEN 1 AND 255),
    INDEX ix_institutional_identity_subject (subject)
);

CREATE TABLE identity_role_assignment (
    assignment_id CHAR(36) NOT NULL,
    target_identity_id CHAR(36) NOT NULL,
    profile_key VARCHAR(24) NOT NULL,
    status VARCHAR(16) NOT NULL,
    valid_from DATE NOT NULL,
    valid_through DATE NULL,
    source_reference VARCHAR(512) NOT NULL,
    granted_by_identity_id CHAR(36) NOT NULL,
    created_at TIMESTAMP(6) NOT NULL,
    version BIGINT NOT NULL,
    PRIMARY KEY (assignment_id),
    CONSTRAINT fk_identity_role_assignment_target
        FOREIGN KEY (target_identity_id) REFERENCES institutional_identity (identity_id),
    CONSTRAINT fk_identity_role_assignment_grantor
        FOREIGN KEY (granted_by_identity_id) REFERENCES institutional_identity (identity_id),
    CONSTRAINT ck_identity_role_assignment_profile CHECK (profile_key IN (
        'APPLICANT', 'ADMITTED', 'STUDENT', 'TEACHER', 'ADMINISTRATIVE', 'ADMISSIONS', 'DIRECTIVE', 'ADMINISTRATOR'
    )),
    CONSTRAINT ck_identity_role_assignment_status CHECK (status IN ('ACTIVE', 'REVOKED')),
    CONSTRAINT ck_identity_role_assignment_dates
        CHECK (valid_through IS NULL OR valid_through >= valid_from),
    CONSTRAINT ck_identity_role_assignment_reference CHECK (CHAR_LENGTH(TRIM(source_reference)) > 0),
    CONSTRAINT ck_identity_role_assignment_version CHECK (version >= 1),
    INDEX ix_identity_role_assignment_target_status (target_identity_id, status, valid_from, valid_through)
);

CREATE TABLE identity_role_assignment_scope (
    assignment_id CHAR(36) NOT NULL,
    scope_kind VARCHAR(24) NOT NULL,
    site_id CHAR(36) NULL,
    organization_unit_id CHAR(36) NULL,
    program_id CHAR(36) NULL,
    job_appointment_reference VARCHAR(256) NULL,
    PRIMARY KEY (assignment_id, scope_kind),
    CONSTRAINT fk_identity_role_scope_assignment
        FOREIGN KEY (assignment_id) REFERENCES identity_role_assignment (assignment_id),
    CONSTRAINT fk_identity_role_scope_site
        FOREIGN KEY (site_id) REFERENCES academic_site (site_id),
    CONSTRAINT fk_identity_role_scope_organization_unit
        FOREIGN KEY (organization_unit_id) REFERENCES academic_organization_unit (organization_unit_id),
    CONSTRAINT fk_identity_role_scope_program
        FOREIGN KEY (program_id) REFERENCES academic_program (program_id),
    CONSTRAINT ck_identity_role_scope_kind CHECK (scope_kind IN (
        'UNIVERSITY', 'SITE', 'FACULTY', 'PROGRAM', 'JOB_APPOINTMENT'
    )),
    CONSTRAINT ck_identity_role_scope_reference CHECK (
        (scope_kind = 'UNIVERSITY' AND site_id IS NULL AND organization_unit_id IS NULL
            AND program_id IS NULL AND job_appointment_reference IS NULL)
        OR (scope_kind = 'SITE' AND site_id IS NOT NULL AND organization_unit_id IS NULL
            AND program_id IS NULL AND job_appointment_reference IS NULL)
        OR (scope_kind = 'FACULTY' AND site_id IS NULL AND organization_unit_id IS NOT NULL
            AND program_id IS NULL AND job_appointment_reference IS NULL)
        OR (scope_kind = 'PROGRAM' AND site_id IS NULL AND organization_unit_id IS NULL
            AND program_id IS NOT NULL AND job_appointment_reference IS NULL)
        OR (scope_kind = 'JOB_APPOINTMENT' AND site_id IS NULL AND organization_unit_id IS NULL
            AND program_id IS NULL AND CHAR_LENGTH(TRIM(job_appointment_reference)) > 0)
    )
);

CREATE TABLE identity_access_audit_event (
    audit_event_id CHAR(36) NOT NULL,
    assignment_id CHAR(36) NOT NULL,
    action_key VARCHAR(16) NOT NULL,
    actor_identity_id CHAR(36) NOT NULL,
    occurred_at TIMESTAMP(6) NOT NULL,
    source_reference VARCHAR(512) NOT NULL,
    previous_version BIGINT NOT NULL,
    version BIGINT NOT NULL,
    PRIMARY KEY (audit_event_id),
    CONSTRAINT fk_identity_access_audit_assignment
        FOREIGN KEY (assignment_id) REFERENCES identity_role_assignment (assignment_id),
    CONSTRAINT fk_identity_access_audit_actor
        FOREIGN KEY (actor_identity_id) REFERENCES institutional_identity (identity_id),
    CONSTRAINT uq_identity_access_audit_assignment_version UNIQUE (assignment_id, version),
    CONSTRAINT ck_identity_access_audit_action CHECK (action_key IN ('GRANTED', 'REVOKED')),
    CONSTRAINT ck_identity_access_audit_reference CHECK (CHAR_LENGTH(TRIM(source_reference)) > 0),
    CONSTRAINT ck_identity_access_audit_transition CHECK (
        previous_version >= 0 AND version = previous_version + 1
        AND ((action_key = 'GRANTED' AND previous_version = 0 AND version = 1)
            OR (action_key = 'REVOKED' AND previous_version >= 1))
    ),
    INDEX ix_identity_access_audit_assignment_time (assignment_id, occurred_at)
);
