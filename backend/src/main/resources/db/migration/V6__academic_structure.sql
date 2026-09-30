CREATE TABLE academic_structure_control (
    control_id SMALLINT NOT NULL,
    PRIMARY KEY (control_id),
    CONSTRAINT ck_academic_structure_control_singleton CHECK (control_id = 1)
);

INSERT INTO academic_structure_control (control_id) VALUES (1);

CREATE TABLE academic_organization_unit (
    organization_unit_id CHAR(36) NOT NULL,
    unit_code VARCHAR(64) NOT NULL,
    unit_type VARCHAR(24) NOT NULL,
    display_name VARCHAR(240) NOT NULL,
    display_order INT NOT NULL,
    status VARCHAR(16) NOT NULL,
    valid_from DATE NOT NULL,
    valid_through DATE NULL,
    created_at TIMESTAMP(6) NOT NULL,
    PRIMARY KEY (organization_unit_id),
    CONSTRAINT uq_academic_organization_unit_code UNIQUE (unit_code),
    CONSTRAINT ck_academic_organization_unit_type
        CHECK (unit_type IN ('FACULTY', 'SCHOOL', 'ACADEMIC_UNIT')),
    CONSTRAINT ck_academic_organization_unit_status CHECK (status IN ('ACTIVE', 'INACTIVE')),
    CONSTRAINT ck_academic_organization_unit_order CHECK (display_order >= 0),
    CONSTRAINT ck_academic_organization_unit_name CHECK (CHAR_LENGTH(TRIM(display_name)) > 0),
    CONSTRAINT ck_academic_organization_unit_dates
        CHECK (valid_through IS NULL OR valid_through >= valid_from),
    INDEX ix_academic_organization_unit_order (status, unit_type, display_order, unit_code)
);

CREATE TABLE academic_organization_relation (
    parent_unit_id CHAR(36) NOT NULL,
    child_unit_id CHAR(36) NOT NULL,
    valid_from DATE NOT NULL,
    valid_through DATE NULL,
    PRIMARY KEY (parent_unit_id, child_unit_id, valid_from),
    CONSTRAINT fk_academic_organization_relation_parent
        FOREIGN KEY (parent_unit_id) REFERENCES academic_organization_unit (organization_unit_id),
    CONSTRAINT fk_academic_organization_relation_child
        FOREIGN KEY (child_unit_id) REFERENCES academic_organization_unit (organization_unit_id),
    CONSTRAINT ck_academic_organization_relation_distinct CHECK (parent_unit_id <> child_unit_id),
    CONSTRAINT ck_academic_organization_relation_dates
        CHECK (valid_through IS NULL OR valid_through >= valid_from),
    INDEX ix_academic_organization_relation_child (child_unit_id, valid_from, valid_through),
    INDEX ix_academic_organization_relation_parent (parent_unit_id, child_unit_id)
);

CREATE TABLE academic_site (
    site_id CHAR(36) NOT NULL,
    site_code VARCHAR(64) NOT NULL,
    site_type VARCHAR(24) NOT NULL,
    display_name VARCHAR(240) NOT NULL,
    display_order INT NOT NULL,
    status VARCHAR(16) NOT NULL,
    valid_from DATE NOT NULL,
    valid_through DATE NULL,
    created_at TIMESTAMP(6) NOT NULL,
    PRIMARY KEY (site_id),
    CONSTRAINT uq_academic_site_code UNIQUE (site_code),
    CONSTRAINT ck_academic_site_type
        CHECK (site_type IN ('CENTRAL', 'SECCIONAL', 'REGIONAL', 'CREAD', 'CAMPUS', 'OTHER')),
    CONSTRAINT ck_academic_site_status CHECK (status IN ('ACTIVE', 'INACTIVE')),
    CONSTRAINT ck_academic_site_order CHECK (display_order >= 0),
    CONSTRAINT ck_academic_site_name CHECK (CHAR_LENGTH(TRIM(display_name)) > 0),
    CONSTRAINT ck_academic_site_dates CHECK (valid_through IS NULL OR valid_through >= valid_from),
    INDEX ix_academic_site_order (status, site_type, display_order, site_code)
);

CREATE TABLE academic_site_relation (
    parent_site_id CHAR(36) NOT NULL,
    child_site_id CHAR(36) NOT NULL,
    valid_from DATE NOT NULL,
    valid_through DATE NULL,
    PRIMARY KEY (parent_site_id, child_site_id, valid_from),
    CONSTRAINT fk_academic_site_relation_parent
        FOREIGN KEY (parent_site_id) REFERENCES academic_site (site_id),
    CONSTRAINT fk_academic_site_relation_child
        FOREIGN KEY (child_site_id) REFERENCES academic_site (site_id),
    CONSTRAINT ck_academic_site_relation_distinct CHECK (parent_site_id <> child_site_id),
    CONSTRAINT ck_academic_site_relation_dates CHECK (valid_through IS NULL OR valid_through >= valid_from),
    INDEX ix_academic_site_relation_child (child_site_id, valid_from, valid_through),
    INDEX ix_academic_site_relation_parent (parent_site_id, child_site_id)
);

CREATE TABLE academic_program_affiliation (
    affiliation_id CHAR(36) NOT NULL,
    program_id CHAR(36) NOT NULL,
    organization_unit_id CHAR(36) NOT NULL,
    site_id CHAR(36) NOT NULL,
    valid_from DATE NOT NULL,
    valid_through DATE NULL,
    source_reference VARCHAR(240) NOT NULL,
    created_by VARCHAR(180) NOT NULL,
    created_at TIMESTAMP(6) NOT NULL,
    PRIMARY KEY (affiliation_id),
    CONSTRAINT uq_academic_program_affiliation_start UNIQUE (program_id, valid_from),
    CONSTRAINT fk_academic_program_affiliation_program
        FOREIGN KEY (program_id) REFERENCES academic_program (program_id),
    CONSTRAINT fk_academic_program_affiliation_unit
        FOREIGN KEY (organization_unit_id) REFERENCES academic_organization_unit (organization_unit_id),
    CONSTRAINT fk_academic_program_affiliation_site
        FOREIGN KEY (site_id) REFERENCES academic_site (site_id),
    CONSTRAINT ck_academic_program_affiliation_dates
        CHECK (valid_through IS NULL OR valid_through >= valid_from),
    CONSTRAINT ck_academic_program_affiliation_reference
        CHECK (CHAR_LENGTH(TRIM(source_reference)) > 0),
    CONSTRAINT ck_academic_program_affiliation_actor CHECK (CHAR_LENGTH(TRIM(created_by)) > 0),
    INDEX ix_academic_program_affiliation_tree (organization_unit_id, site_id, valid_from, valid_through),
    INDEX ix_academic_program_affiliation_program (program_id, valid_from, valid_through)
);

CREATE TABLE academic_structure_audit_event (
    audit_event_id BIGINT NOT NULL AUTO_INCREMENT,
    entity_id CHAR(36) NOT NULL,
    action_key VARCHAR(32) NOT NULL,
    actor_sub VARCHAR(180) NOT NULL,
    source_reference VARCHAR(240) NOT NULL,
    occurred_at TIMESTAMP(6) NOT NULL,
    event_summary VARCHAR(240) NOT NULL,
    PRIMARY KEY (audit_event_id),
    CONSTRAINT ck_academic_structure_audit_action
        CHECK (action_key IN ('UNIT_CREATED', 'UNIT_RELATED', 'SITE_CREATED', 'SITE_RELATED', 'PROGRAM_AFFILIATED')),
    CONSTRAINT ck_academic_structure_audit_actor CHECK (CHAR_LENGTH(TRIM(actor_sub)) > 0),
    CONSTRAINT ck_academic_structure_audit_reference CHECK (CHAR_LENGTH(TRIM(source_reference)) > 0),
    CONSTRAINT ck_academic_structure_audit_summary CHECK (CHAR_LENGTH(TRIM(event_summary)) > 0),
    INDEX ix_academic_structure_audit_entity_time (entity_id, occurred_at)
);
