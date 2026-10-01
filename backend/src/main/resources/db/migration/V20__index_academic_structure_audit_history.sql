CREATE INDEX ix_academic_structure_audit_occurred_at
    ON academic_structure_audit_event (occurred_at);

CREATE INDEX ix_academic_structure_audit_action_time
    ON academic_structure_audit_event (action_key, occurred_at);
