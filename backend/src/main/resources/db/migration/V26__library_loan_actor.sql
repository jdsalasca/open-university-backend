-- A loan now records who opened it. The closure trail already stored the acting subject, but lending was anonymous:
-- there was no way to answer who handed a copy over, only who took it back.
ALTER TABLE library_loan
    ADD COLUMN created_by VARCHAR(255) NULL;

-- Existing rows predate the trail. Backfill from the value the closure check already guaranteed when present, and
-- label the rest explicitly so the column never stays silently empty. New loans always receive the acting subject.
UPDATE library_loan
SET created_by = COALESCE(closed_by, 'legacy-unknown-actor')
WHERE created_by IS NULL;

ALTER TABLE library_loan
    ADD CONSTRAINT ck_library_loan_actor CHECK (
        created_by IS NOT NULL AND CHAR_LENGTH(TRIM(created_by)) > 0
    );