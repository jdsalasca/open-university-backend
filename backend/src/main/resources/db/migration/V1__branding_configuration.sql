CREATE TABLE media_asset (
    asset_id CHAR(36) NOT NULL,
    storage_key VARCHAR(128) NOT NULL,
    mime_type VARCHAR(64) NOT NULL,
    size_bytes BIGINT NOT NULL,
    width_px INT NOT NULL,
    height_px INT NOT NULL,
    sha256 CHAR(64) NOT NULL,
    uploaded_by VARCHAR(180) NOT NULL,
    uploaded_at TIMESTAMP(6) NOT NULL,
    PRIMARY KEY (asset_id),
    CONSTRAINT uq_media_asset_storage_key UNIQUE (storage_key),
    CONSTRAINT ck_media_asset_size CHECK (size_bytes > 0),
    CONSTRAINT ck_media_asset_dimensions CHECK (width_px > 0 AND height_px > 0)
);

CREATE TABLE institution_branding_revision (
    revision_id BIGINT NOT NULL,
    institution_name VARCHAR(240) NOT NULL,
    logo_light_asset_id CHAR(36) NULL,
    logo_dark_asset_id CHAR(36) NULL,
    favicon_asset_id CHAR(36) NULL,
    created_by VARCHAR(180) NOT NULL,
    created_at TIMESTAMP(6) NOT NULL,
    source_revision_id BIGINT NULL,
    PRIMARY KEY (revision_id),
    CONSTRAINT fk_brand_revision_source FOREIGN KEY (source_revision_id)
        REFERENCES institution_branding_revision (revision_id),
    CONSTRAINT fk_brand_revision_logo_light FOREIGN KEY (logo_light_asset_id)
        REFERENCES media_asset (asset_id),
    CONSTRAINT fk_brand_revision_logo_dark FOREIGN KEY (logo_dark_asset_id)
        REFERENCES media_asset (asset_id),
    CONSTRAINT fk_brand_revision_favicon FOREIGN KEY (favicon_asset_id)
        REFERENCES media_asset (asset_id),
    CONSTRAINT ck_brand_revision_name CHECK (LENGTH(TRIM(institution_name)) > 0)
);

CREATE TABLE institution_branding_current (
    singleton_id INT NOT NULL,
    revision_id BIGINT NOT NULL,
    row_version BIGINT NOT NULL DEFAULT 0,
    PRIMARY KEY (singleton_id),
    CONSTRAINT ck_branding_current_singleton CHECK (singleton_id = 1),
    CONSTRAINT fk_branding_current_revision FOREIGN KEY (revision_id)
        REFERENCES institution_branding_revision (revision_id)
);

CREATE TABLE institution_color_token (
    color_token_id BIGINT NOT NULL AUTO_INCREMENT,
    revision_id BIGINT NOT NULL,
    token_key VARCHAR(32) NOT NULL,
    color_hex CHAR(7) NOT NULL,
    PRIMARY KEY (color_token_id),
    CONSTRAINT uq_color_token_revision_key UNIQUE (revision_id, token_key),
    CONSTRAINT fk_color_token_revision FOREIGN KEY (revision_id)
        REFERENCES institution_branding_revision (revision_id),
    CONSTRAINT ck_color_token_key CHECK (token_key IN ('primary', 'ink', 'surface', 'text', 'accent', 'focus'))
);

CREATE TABLE institution_module_label (
    module_label_id BIGINT NOT NULL AUTO_INCREMENT,
    revision_id BIGINT NOT NULL,
    module_key VARCHAR(64) NOT NULL,
    label VARCHAR(100) NOT NULL,
    available BOOLEAN NOT NULL,
    visible BOOLEAN NOT NULL,
    display_order INT NOT NULL,
    PRIMARY KEY (module_label_id),
    CONSTRAINT uq_module_label_revision_key UNIQUE (revision_id, module_key),
    CONSTRAINT fk_module_label_revision FOREIGN KEY (revision_id)
        REFERENCES institution_branding_revision (revision_id),
    CONSTRAINT ck_module_label_key CHECK (module_key IN ('home', 'students', 'programs', 'curricula', 'subjects', 'academic-load', 'visual-identity')),
    CONSTRAINT ck_module_label_text CHECK (LENGTH(TRIM(label)) > 0),
    CONSTRAINT ck_module_label_order CHECK (display_order >= 0)
);

CREATE TABLE institution_banner (
    institution_banner_id BIGINT NOT NULL AUTO_INCREMENT,
    revision_id BIGINT NOT NULL,
    banner_key CHAR(36) NOT NULL,
    asset_id CHAR(36) NOT NULL,
    title VARCHAR(160) NOT NULL,
    alt_text VARCHAR(300) NOT NULL,
    placement VARCHAR(64) NOT NULL,
    display_order INT NOT NULL,
    starts_at TIMESTAMP(6) NULL,
    ends_at TIMESTAMP(6) NULL,
    PRIMARY KEY (institution_banner_id),
    CONSTRAINT uq_banner_revision_key UNIQUE (revision_id, banner_key),
    CONSTRAINT fk_banner_revision FOREIGN KEY (revision_id)
        REFERENCES institution_branding_revision (revision_id),
    CONSTRAINT fk_banner_asset FOREIGN KEY (asset_id)
        REFERENCES media_asset (asset_id),
    CONSTRAINT ck_banner_title CHECK (LENGTH(TRIM(title)) > 0),
    CONSTRAINT ck_banner_alt_text CHECK (LENGTH(TRIM(alt_text)) > 0),
    CONSTRAINT ck_banner_order CHECK (display_order >= 0),
    CONSTRAINT ck_banner_dates CHECK (ends_at IS NULL OR starts_at IS NULL OR ends_at > starts_at)
);

CREATE TABLE administrative_audit_event (
    audit_event_id BIGINT NOT NULL AUTO_INCREMENT,
    actor_sub VARCHAR(180) NOT NULL,
    action_key VARCHAR(80) NOT NULL,
    entity_type VARCHAR(80) NOT NULL,
    revision_id BIGINT NOT NULL,
    occurred_at TIMESTAMP(6) NOT NULL,
    change_summary TEXT NOT NULL,
    PRIMARY KEY (audit_event_id),
    CONSTRAINT fk_audit_brand_revision FOREIGN KEY (revision_id)
        REFERENCES institution_branding_revision (revision_id),
    INDEX ix_audit_revision_time (revision_id, occurred_at)
);

INSERT INTO institution_branding_revision (
    revision_id, institution_name, created_by, created_at, source_revision_id
) VALUES (
    1, 'Universidad Pedagógica y Tecnológica de Colombia', 'system:bootstrap', CURRENT_TIMESTAMP(6), NULL
);

INSERT INTO institution_branding_current (singleton_id, revision_id, row_version) VALUES (1, 1, 0);

INSERT INTO institution_color_token (revision_id, token_key, color_hex) VALUES
    (1, 'primary', '#FFCC29'),
    (1, 'ink', '#1A1A1A'),
    (1, 'surface', '#FFFFFF'),
    (1, 'text', '#1A1A1A'),
    (1, 'accent', '#FFCC29'),
    (1, 'focus', '#1A1A1A');

INSERT INTO institution_module_label (revision_id, module_key, label, available, visible, display_order) VALUES
    (1, 'home', 'Inicio', TRUE, TRUE, 10),
    (1, 'students', 'Estudiantes', FALSE, FALSE, 20),
    (1, 'programs', 'Programas', FALSE, FALSE, 30),
    (1, 'curricula', 'Mallas curriculares', FALSE, FALSE, 40),
    (1, 'subjects', 'Asignaturas', FALSE, FALSE, 50),
    (1, 'academic-load', 'Carga académica', FALSE, FALSE, 60),
    (1, 'visual-identity', 'Identidad visual', TRUE, TRUE, 90);
