ALTER TABLE academic_organization_relation
    ADD COLUMN display_order INT NOT NULL DEFAULT 0;

ALTER TABLE academic_organization_relation
    ADD CONSTRAINT ck_academic_organization_relation_order CHECK (display_order >= 0);

UPDATE academic_organization_relation
SET display_order = (
    SELECT display_order
    FROM academic_organization_unit
    WHERE organization_unit_id = academic_organization_relation.child_unit_id
);

CREATE INDEX ix_academic_organization_relation_order
    ON academic_organization_relation (parent_unit_id, display_order, child_unit_id, valid_from, valid_through);

ALTER TABLE academic_site_relation
    ADD COLUMN display_order INT NOT NULL DEFAULT 0;

ALTER TABLE academic_site_relation
    ADD CONSTRAINT ck_academic_site_relation_order CHECK (display_order >= 0);

UPDATE academic_site_relation
SET display_order = (
    SELECT display_order
    FROM academic_site
    WHERE site_id = academic_site_relation.child_site_id
);

CREATE INDEX ix_academic_site_relation_order
    ON academic_site_relation (parent_site_id, display_order, child_site_id, valid_from, valid_through);
