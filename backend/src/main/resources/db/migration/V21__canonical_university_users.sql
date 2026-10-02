CREATE TABLE university_user (
    user_id CHAR(36) NOT NULL,
    created_at TIMESTAMP(6) NOT NULL,
    PRIMARY KEY (user_id)
);

INSERT INTO university_user (user_id, created_at)
SELECT identity_id, first_seen_at
FROM institutional_identity;

ALTER TABLE institutional_identity
    ADD COLUMN user_id CHAR(36) NULL;

UPDATE institutional_identity
SET user_id = identity_id;

ALTER TABLE institutional_identity
    MODIFY COLUMN user_id CHAR(36) NOT NULL;

ALTER TABLE institutional_identity
    ADD CONSTRAINT fk_institutional_identity_user
        FOREIGN KEY (user_id) REFERENCES university_user (user_id);

CREATE INDEX ix_institutional_identity_user ON institutional_identity (user_id);

ALTER TABLE identity_role_assignment
    ADD COLUMN target_user_id CHAR(36) NULL;

ALTER TABLE identity_role_assignment
    ADD COLUMN granted_by_user_id CHAR(36) NULL;

UPDATE identity_role_assignment ra
SET target_user_id = (
        SELECT target.user_id
        FROM institutional_identity target
        WHERE target.identity_id = ra.target_identity_id
    ),
    granted_by_user_id = (
        SELECT grantor.user_id
        FROM institutional_identity grantor
        WHERE grantor.identity_id = ra.granted_by_identity_id
    );

ALTER TABLE identity_role_assignment
    MODIFY COLUMN target_user_id CHAR(36) NOT NULL;

ALTER TABLE identity_role_assignment
    MODIFY COLUMN granted_by_user_id CHAR(36) NOT NULL;

ALTER TABLE identity_role_assignment
    ADD CONSTRAINT fk_identity_role_assignment_target_user
        FOREIGN KEY (target_user_id) REFERENCES university_user (user_id);

ALTER TABLE identity_role_assignment
    ADD CONSTRAINT fk_identity_role_assignment_grantor_user
        FOREIGN KEY (granted_by_user_id) REFERENCES university_user (user_id);

CREATE INDEX ix_identity_role_assignment_user_status
    ON identity_role_assignment (target_user_id, status, valid_from, valid_through);

ALTER TABLE identity_access_audit_event
    ADD COLUMN actor_user_id CHAR(36) NULL;

UPDATE identity_access_audit_event ae
SET actor_user_id = (
    SELECT actor.user_id
    FROM institutional_identity actor
    WHERE actor.identity_id = ae.actor_identity_id
);

ALTER TABLE identity_access_audit_event
    MODIFY COLUMN actor_user_id CHAR(36) NOT NULL;

ALTER TABLE identity_access_audit_event
    ADD CONSTRAINT fk_identity_access_audit_actor_user
        FOREIGN KEY (actor_user_id) REFERENCES university_user (user_id);

ALTER TABLE identity_role_assignment
    DROP CONSTRAINT fk_identity_role_assignment_target;

ALTER TABLE identity_role_assignment
    DROP CONSTRAINT fk_identity_role_assignment_grantor;

ALTER TABLE identity_role_assignment
    DROP INDEX ix_identity_role_assignment_target_status;

ALTER TABLE identity_role_assignment
    DROP COLUMN target_identity_id;

ALTER TABLE identity_role_assignment
    DROP COLUMN granted_by_identity_id;
