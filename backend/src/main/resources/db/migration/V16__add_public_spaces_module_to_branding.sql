ALTER TABLE institution_module_label
    DROP CONSTRAINT ck_module_label_key;

ALTER TABLE institution_module_label
    ADD CONSTRAINT ck_module_label_key CHECK (
        module_key IN ('home', 'students', 'programs', 'curricula', 'subjects', 'academic-load', 'spaces', 'admissions', 'visual-identity')
    );

INSERT INTO institution_module_label (
    revision_id, module_key, label, available, visible, display_order
)
SELECT
    branding_revision.revision_id,
    'spaces',
    'Guía de espacios',
    TRUE,
    TRUE,
    70
FROM institution_branding_revision branding_revision
WHERE NOT EXISTS (
    SELECT 1
    FROM institution_module_label module_label
    WHERE module_label.revision_id = branding_revision.revision_id
      AND module_label.module_key = 'spaces'
);
