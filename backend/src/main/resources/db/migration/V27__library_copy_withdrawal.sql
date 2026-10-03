-- Withdrawal closes the dead `active` flag: the lending check already refused inactive copies, but nothing could ever
-- make one inactive. A copy leaves circulation under an institutional reference and the acting subject.
ALTER TABLE library_copy
    ADD COLUMN withdrawn_by VARCHAR(255) NULL;

ALTER TABLE library_copy
    ADD COLUMN withdrawn_reference VARCHAR(240) NULL;

ALTER TABLE library_copy
    ADD COLUMN withdrawn_at TIMESTAMP(6) NULL;

-- A circulating copy carries no withdrawal trail; a withdrawn copy carries all of it.
ALTER TABLE library_copy
    ADD CONSTRAINT ck_library_copy_withdrawal CHECK (
        (active = TRUE AND withdrawn_by IS NULL AND withdrawn_reference IS NULL AND withdrawn_at IS NULL)
        OR (active = FALSE AND withdrawn_at IS NOT NULL
            AND CHAR_LENGTH(TRIM(withdrawn_by)) > 0
            AND CHAR_LENGTH(TRIM(withdrawn_reference)) > 0)
    );
