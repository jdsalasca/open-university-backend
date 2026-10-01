ALTER TABLE institution_module_label
    DROP CONSTRAINT ck_module_label_key;

ALTER TABLE institution_module_label
    ADD CONSTRAINT ck_module_label_key CHECK (
        module_key IN ('home', 'students', 'programs', 'curricula', 'subjects', 'academic-load', 'admissions', 'visual-identity')
    );

INSERT INTO institution_module_label (
    revision_id, module_key, label, available, visible, display_order
)
SELECT
    branding_revision.revision_id,
    'admissions',
    'Admisiones',
    TRUE,
    TRUE,
    COALESCE(MAX(module_label.display_order), 0) + 10
FROM institution_branding_revision branding_revision
LEFT JOIN institution_module_label module_label
    ON module_label.revision_id = branding_revision.revision_id
GROUP BY branding_revision.revision_id;
