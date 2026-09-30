ALTER TABLE academic_curriculum
    DROP INDEX ix_academic_curriculum_drafts;

CREATE INDEX ix_academic_curriculum_drafts
    ON academic_curriculum (status, created_at, curriculum_id);
