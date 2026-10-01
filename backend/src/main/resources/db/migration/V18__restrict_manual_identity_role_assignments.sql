ALTER TABLE identity_role_assignment
    ADD CONSTRAINT ck_identity_role_assignment_manual_profile CHECK (profile_key IN (
        'TEACHER', 'ADMINISTRATIVE', 'ADMISSIONS', 'DIRECTIVE', 'ADMINISTRATOR'
    ));
