ALTER TABLE academic_period
    ADD COLUMN approval_reference VARCHAR(240) NULL;

UPDATE academic_period
SET approval_reference = (
    SELECT c.official_reference
    FROM academic_calendar_revision c
    WHERE c.calendar_revision_id = academic_period.approved_calendar_revision_id
      AND c.period_id = academic_period.period_id
)
WHERE status IN ('APPROVED', 'OPEN', 'CLOSED', 'CANCELLED')
  AND approved_calendar_revision_id IS NOT NULL;

ALTER TABLE academic_period
    ADD CONSTRAINT ck_academic_period_approval_reference CHECK (
        (status = 'DRAFT' AND approval_reference IS NULL)
        OR (status IN ('APPROVED', 'OPEN', 'CLOSED')
            AND approval_reference IS NOT NULL AND CHAR_LENGTH(TRIM(approval_reference)) > 0)
        OR (status = 'CANCELLED' AND (approval_reference IS NULL
            OR CHAR_LENGTH(TRIM(approval_reference)) > 0))
    );

ALTER TABLE academic_program_affiliation
    ADD COLUMN display_order INT NOT NULL DEFAULT 0;

ALTER TABLE academic_program_affiliation
    ADD CONSTRAINT ck_academic_program_affiliation_display_order CHECK (display_order >= 0);

CREATE INDEX ix_academic_program_affiliation_order
    ON academic_program_affiliation (organization_unit_id, display_order, program_id);
