ALTER TABLE academic_curriculum_entry
    ADD COLUMN search_subject_code VARCHAR(64) NOT NULL DEFAULT '';

ALTER TABLE academic_curriculum_entry
    ADD COLUMN search_subject_name VARCHAR(240) NOT NULL DEFAULT '';

UPDATE academic_curriculum_entry
SET search_subject_code = (
        SELECT subject_code
        FROM academic_subject
        WHERE academic_subject.subject_id = academic_curriculum_entry.subject_id
    ),
    search_subject_name = (
        SELECT subject_name
        FROM academic_subject_revision
        WHERE academic_subject_revision.subject_revision_id = academic_curriculum_entry.subject_revision_id
          AND academic_subject_revision.subject_id = academic_curriculum_entry.subject_id
    );

ALTER TABLE academic_curriculum_entry
    ADD CONSTRAINT ck_academic_curriculum_entry_search_code
        CHECK (CHAR_LENGTH(TRIM(search_subject_code)) > 0);

ALTER TABLE academic_curriculum_entry
    ADD CONSTRAINT ck_academic_curriculum_entry_search_name
        CHECK (CHAR_LENGTH(TRIM(search_subject_name)) > 0);

ALTER TABLE academic_curriculum_entry
    DROP INDEX ix_academic_curriculum_entry_order_lookup;

CREATE INDEX ix_academic_curriculum_entry_search_order_lookup
    ON academic_curriculum_entry (
        curriculum_id,
        semester,
        row_order,
        search_subject_code,
        search_subject_name
    );
