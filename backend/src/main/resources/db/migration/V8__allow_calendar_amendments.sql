ALTER TABLE academic_period_audit_event
    DROP CONSTRAINT ck_academic_period_audit_action;

ALTER TABLE academic_period_audit_event
    ADD CONSTRAINT ck_academic_period_audit_action
        CHECK (action_key IN ('PERIOD_CREATED', 'CALENDAR_CREATED', 'CALENDAR_PUBLISHED',
            'PERIOD_APPROVED', 'PERIOD_OPENED', 'PERIOD_CLOSED', 'PERIOD_CANCELLED',
            'PERIOD_CALENDAR_AMENDED'));
