CREATE TABLE institutional_notice (
    notice_id CHAR(36) NOT NULL,
    title VARCHAR(160) NOT NULL,
    body VARCHAR(2000) NOT NULL,
    source_reference VARCHAR(240) NOT NULL,
    published_from DATE NOT NULL,
    published_through DATE NOT NULL,
    published_by VARCHAR(255) NOT NULL,
    published_at TIMESTAMP(6) NOT NULL,
    PRIMARY KEY (notice_id),
    CONSTRAINT ck_institutional_notice_window CHECK (published_through >= published_from),
    CONSTRAINT ck_institutional_notice_title CHECK (CHAR_LENGTH(TRIM(title)) > 0),
    CONSTRAINT ck_institutional_notice_body CHECK (CHAR_LENGTH(TRIM(body)) > 0),
    CONSTRAINT ck_institutional_notice_reference CHECK (CHAR_LENGTH(TRIM(source_reference)) > 0),
    CONSTRAINT ck_institutional_notice_actor CHECK (CHAR_LENGTH(TRIM(published_by)) > 0),
    INDEX ix_institutional_notice_publication (published_at, notice_id)
);

CREATE TABLE institutional_notice_audience (
    notice_id CHAR(36) NOT NULL,
    audience_kind VARCHAR(16) NOT NULL,
    -- The primary key implies NOT NULL in MySQL, so the university scope uses the empty reference instead of NULL.
    scope_reference VARCHAR(64) NOT NULL DEFAULT '',
    PRIMARY KEY (notice_id, audience_kind, scope_reference),
    CONSTRAINT fk_institutional_notice_audience_notice
        FOREIGN KEY (notice_id) REFERENCES institutional_notice (notice_id),
    CONSTRAINT ck_institutional_notice_audience_kind
        CHECK (audience_kind IN ('UNIVERSITY', 'SITE', 'FACULTY', 'PROGRAM')),
    -- The university scope is the whole community, so it never points at one unit, site or program.
    CONSTRAINT ck_institutional_notice_audience_scope CHECK (
        (audience_kind = 'UNIVERSITY' AND scope_reference = '')
        OR (audience_kind <> 'UNIVERSITY' AND CHAR_LENGTH(TRIM(scope_reference)) > 0)
    )
);

CREATE TABLE institutional_notice_audit_event (
    audit_event_id BIGINT NOT NULL AUTO_INCREMENT,
    notice_id CHAR(36) NOT NULL,
    action_key VARCHAR(32) NOT NULL,
    actor_sub VARCHAR(255) NOT NULL,
    occurred_at TIMESTAMP(6) NOT NULL,
    source_reference VARCHAR(240) NOT NULL,
    audience_count INT NOT NULL,
    PRIMARY KEY (audit_event_id),
    CONSTRAINT fk_institutional_notice_audit_notice
        FOREIGN KEY (notice_id) REFERENCES institutional_notice (notice_id),
    CONSTRAINT ck_institutional_notice_audit_action CHECK (action_key = 'NOTICE_PUBLISHED'),
    CONSTRAINT ck_institutional_notice_audit_actor CHECK (CHAR_LENGTH(TRIM(actor_sub)) > 0),
    CONSTRAINT ck_institutional_notice_audit_reference CHECK (CHAR_LENGTH(TRIM(source_reference)) > 0),
    CONSTRAINT ck_institutional_notice_audit_audience_count CHECK (audience_count >= 1),
    INDEX ix_institutional_notice_audit_history (notice_id, audit_event_id)
);