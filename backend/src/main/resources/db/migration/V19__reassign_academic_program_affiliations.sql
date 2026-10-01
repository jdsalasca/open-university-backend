ALTER TABLE academic_structure_audit_event
    DROP CONSTRAINT ck_academic_structure_audit_action;

ALTER TABLE academic_structure_audit_event
    ADD CONSTRAINT ck_academic_structure_audit_action
        CHECK (action_key IN (
            'UNIT_CREATED',
            'UNIT_RELATED',
            'SITE_CREATED',
            'SITE_RELATED',
            'PROGRAM_AFFILIATED',
            'UNIT_ORDER_CHANGED',
            'SITE_ORDER_CHANGED',
            'UNIT_RELATION_ORDER_CHANGED',
            'SITE_RELATION_ORDER_CHANGED',
            'PROGRAM_ORDER_CHANGED',
            'UNIT_RELATION_CLOSED',
            'SITE_RELATION_CLOSED',
            'PROGRAM_AFFILIATION_CLOSED',
            'PROGRAM_AFFILIATION_REASSIGNED'
        ));
