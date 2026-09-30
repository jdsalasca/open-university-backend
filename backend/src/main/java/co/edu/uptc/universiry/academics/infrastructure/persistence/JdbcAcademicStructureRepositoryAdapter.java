package co.edu.uptc.universiry.academics.infrastructure.persistence;

import co.edu.uptc.universiry.academics.application.AcademicStructureConflictException;
import co.edu.uptc.universiry.academics.application.AcademicStructureNotFoundException;
import co.edu.uptc.universiry.academics.application.AcademicStructureRepository;
import co.edu.uptc.universiry.academics.domain.AcademicEntityStatus;
import co.edu.uptc.universiry.academics.domain.AcademicOrganizationRelation;
import co.edu.uptc.universiry.academics.domain.AcademicOrganizationUnit;
import co.edu.uptc.universiry.academics.domain.AcademicOrganizationUnitType;
import co.edu.uptc.universiry.academics.domain.AcademicProgramAffiliation;
import co.edu.uptc.universiry.academics.domain.AcademicSite;
import co.edu.uptc.universiry.academics.domain.AcademicSiteRelation;
import co.edu.uptc.universiry.academics.domain.AcademicSiteType;
import co.edu.uptc.universiry.academics.domain.AcademicStructureRules;
import co.edu.uptc.universiry.academics.domain.AcademicStructureSnapshot;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Repository
public class JdbcAcademicStructureRepositoryAdapter implements AcademicStructureRepository {

    private static final RowMapper<AcademicOrganizationUnit> UNIT_MAPPER = (rs, row) -> new AcademicOrganizationUnit(
            uuid(rs.getString("organization_unit_id")), rs.getString("unit_code"),
            AcademicOrganizationUnitType.valueOf(rs.getString("unit_type")), rs.getString("display_name"),
            rs.getInt("display_order"), AcademicEntityStatus.valueOf(rs.getString("status")),
            localDate(rs, "valid_from"), nullableDate(rs, "valid_through"));
    private static final RowMapper<AcademicSite> SITE_MAPPER = (rs, row) -> new AcademicSite(
            uuid(rs.getString("site_id")), rs.getString("site_code"),
            AcademicSiteType.valueOf(rs.getString("site_type")), rs.getString("display_name"),
            rs.getInt("display_order"), AcademicEntityStatus.valueOf(rs.getString("status")),
            localDate(rs, "valid_from"), nullableDate(rs, "valid_through"));
    private static final RowMapper<AcademicOrganizationRelation> ORGANIZATION_RELATION_MAPPER = (rs, row) ->
            new AcademicOrganizationRelation(uuid(rs.getString("parent_unit_id")),
                    uuid(rs.getString("child_unit_id")), localDate(rs, "valid_from"),
                    nullableDate(rs, "valid_through"));
    private static final RowMapper<AcademicSiteRelation> SITE_RELATION_MAPPER = (rs, row) ->
            new AcademicSiteRelation(uuid(rs.getString("parent_site_id")), uuid(rs.getString("child_site_id")),
                    localDate(rs, "valid_from"), nullableDate(rs, "valid_through"));
    private static final RowMapper<AcademicProgramAffiliation> AFFILIATION_MAPPER = (rs, row) ->
            new AcademicProgramAffiliation(uuid(rs.getString("affiliation_id")),
                    uuid(rs.getString("program_id")), uuid(rs.getString("organization_unit_id")),
                    uuid(rs.getString("site_id")), rs.getInt("display_order"), localDate(rs, "valid_from"),
                    nullableDate(rs, "valid_through"), rs.getString("source_reference"));

    private final JdbcTemplate jdbcTemplate;
    private final Clock clock;

    public JdbcAcademicStructureRepositoryAdapter(JdbcTemplate jdbcTemplate, Clock clock) {
        this.jdbcTemplate = jdbcTemplate;
        this.clock = clock;
    }

    @Override
    @Transactional(readOnly = true)
    public AcademicStructureSnapshot findPublicStructure(LocalDate asOf) {
        List<AcademicOrganizationUnit> units = jdbcTemplate.query("""
                SELECT organization_unit_id, unit_code, unit_type, display_name, display_order, status,
                       valid_from, valid_through
                FROM academic_organization_unit
                WHERE status = 'ACTIVE' AND valid_from <= ? AND (valid_through IS NULL OR valid_through >= ?)
                ORDER BY display_order, unit_type, unit_code
                """, UNIT_MAPPER, asOf, asOf);
        List<AcademicOrganizationRelation> organizationRelations = jdbcTemplate.query("""
                SELECT r.parent_unit_id, r.child_unit_id, r.valid_from, r.valid_through
                FROM academic_organization_relation r
                JOIN academic_organization_unit p ON p.organization_unit_id = r.parent_unit_id
                JOIN academic_organization_unit c ON c.organization_unit_id = r.child_unit_id
                WHERE p.status = 'ACTIVE' AND c.status = 'ACTIVE'
                  AND p.valid_from <= ? AND (p.valid_through IS NULL OR p.valid_through >= ?)
                  AND c.valid_from <= ? AND (c.valid_through IS NULL OR c.valid_through >= ?)
                  AND r.valid_from <= ? AND (r.valid_through IS NULL OR r.valid_through >= ?)
                ORDER BY p.display_order, c.display_order, p.unit_code, c.unit_code
                """, ORGANIZATION_RELATION_MAPPER, asOf, asOf, asOf, asOf, asOf, asOf);
        List<AcademicSite> sites = jdbcTemplate.query("""
                SELECT site_id, site_code, site_type, display_name, display_order, status, valid_from, valid_through
                FROM academic_site
                WHERE status = 'ACTIVE' AND valid_from <= ? AND (valid_through IS NULL OR valid_through >= ?)
                ORDER BY display_order, site_type, site_code
                """, SITE_MAPPER, asOf, asOf);
        List<AcademicSiteRelation> siteRelations = jdbcTemplate.query("""
                SELECT r.parent_site_id, r.child_site_id, r.valid_from, r.valid_through
                FROM academic_site_relation r
                JOIN academic_site p ON p.site_id = r.parent_site_id
                JOIN academic_site c ON c.site_id = r.child_site_id
                WHERE p.status = 'ACTIVE' AND c.status = 'ACTIVE'
                  AND p.valid_from <= ? AND (p.valid_through IS NULL OR p.valid_through >= ?)
                  AND c.valid_from <= ? AND (c.valid_through IS NULL OR c.valid_through >= ?)
                  AND r.valid_from <= ? AND (r.valid_through IS NULL OR r.valid_through >= ?)
                ORDER BY p.display_order, c.display_order, p.site_code, c.site_code
                """, SITE_RELATION_MAPPER, asOf, asOf, asOf, asOf, asOf, asOf);
        List<AcademicProgramAffiliation> affiliations = jdbcTemplate.query("""
                SELECT a.affiliation_id, a.program_id, a.organization_unit_id, a.site_id,
                       a.display_order, a.valid_from, a.valid_through, a.source_reference
                FROM academic_program_affiliation a
                JOIN academic_organization_unit u ON u.organization_unit_id = a.organization_unit_id
                JOIN academic_site s ON s.site_id = a.site_id
                JOIN academic_program p ON p.program_id = a.program_id
                WHERE u.status = 'ACTIVE' AND s.status = 'ACTIVE'
                  AND u.valid_from <= ? AND (u.valid_through IS NULL OR u.valid_through >= ?)
                  AND s.valid_from <= ? AND (s.valid_through IS NULL OR s.valid_through >= ?)
                  AND a.valid_from <= ? AND (a.valid_through IS NULL OR a.valid_through >= ?)
                ORDER BY a.display_order, p.program_code, p.academic_level, p.study_modality, p.campus_code
                """, AFFILIATION_MAPPER, asOf, asOf, asOf, asOf, asOf, asOf);
        return new AcademicStructureSnapshot(units, organizationRelations, sites, siteRelations, affiliations);
    }

    @Override
    @Transactional(readOnly = true)
    public AcademicStructureSnapshot findAdminStructure() {
        List<AcademicOrganizationUnit> units = jdbcTemplate.query("""
                SELECT organization_unit_id, unit_code, unit_type, display_name, display_order, status,
                       valid_from, valid_through
                FROM academic_organization_unit
                ORDER BY display_order, unit_type, unit_code
                """, UNIT_MAPPER);
        List<AcademicOrganizationRelation> organizationRelations = jdbcTemplate.query("""
                SELECT parent_unit_id, child_unit_id, valid_from, valid_through
                FROM academic_organization_relation
                ORDER BY parent_unit_id, child_unit_id, valid_from
                """, ORGANIZATION_RELATION_MAPPER);
        List<AcademicSite> sites = jdbcTemplate.query("""
                SELECT site_id, site_code, site_type, display_name, display_order, status, valid_from, valid_through
                FROM academic_site
                ORDER BY display_order, site_type, site_code
                """, SITE_MAPPER);
        List<AcademicSiteRelation> siteRelations = jdbcTemplate.query("""
                SELECT parent_site_id, child_site_id, valid_from, valid_through
                FROM academic_site_relation
                ORDER BY parent_site_id, child_site_id, valid_from
                """, SITE_RELATION_MAPPER);
        List<AcademicProgramAffiliation> affiliations = jdbcTemplate.query("""
                SELECT a.affiliation_id, a.program_id, a.organization_unit_id, a.site_id,
                       a.display_order, a.valid_from, a.valid_through, a.source_reference
                FROM academic_program_affiliation a
                JOIN academic_program p ON p.program_id = a.program_id
                ORDER BY a.display_order, p.program_code, p.academic_level, p.study_modality, p.campus_code, a.valid_from
                """, AFFILIATION_MAPPER);
        return new AcademicStructureSnapshot(units, organizationRelations, sites, siteRelations, affiliations);
    }

    @Override
    @Transactional
    public void createOrganizationUnit(AcademicOrganizationUnit unit, String actorSub, String sourceReference) {
        lockStructure();
        jdbcTemplate.update("""
                INSERT INTO academic_organization_unit
                    (organization_unit_id, unit_code, unit_type, display_name, display_order, status,
                     valid_from, valid_through, created_at)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
                """, unit.id().toString(), unit.code(), unit.type().name(), unit.displayName(), unit.displayOrder(),
                unit.status().name(), unit.validFrom(), unit.validThrough(), timestamp(clock.instant()));
        audit(unit.id(), "UNIT_CREATED", actorSub, sourceReference, "Organization unit created: " + unit.code());
    }

    @Override
    @Transactional
    public void createSite(AcademicSite site, String actorSub, String sourceReference) {
        lockStructure();
        jdbcTemplate.update("""
                INSERT INTO academic_site
                    (site_id, site_code, site_type, display_name, display_order, status,
                     valid_from, valid_through, created_at)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
                """, site.id().toString(), site.code(), site.type().name(), site.displayName(), site.displayOrder(),
                site.status().name(), site.validFrom(), site.validThrough(), timestamp(clock.instant()));
        audit(site.id(), "SITE_CREATED", actorSub, sourceReference, "Academic site created: " + site.code());
    }

    @Override
    @Transactional
    public void relateOrganizationUnits(AcademicOrganizationRelation relation, String actorSub, String sourceReference) {
        lockStructure();
        Validity parent = requireActiveUnit(relation.parentUnitId());
        Validity child = requireActiveUnit(relation.childUnitId());
        requireContained(relation.validFrom(), relation.validThrough(), parent);
        requireContained(relation.validFrom(), relation.validThrough(), child);
        List<AcademicOrganizationRelation> existing = allOrganizationRelations();
        ensureSingleParent(relation.childUnitId(), relation.validFrom(), relation.validThrough(),
                existing.stream().map(edge -> new ExistingRelation(edge.childUnitId(), edge.validFrom(), edge.validThrough())).toList());
        if (AcademicStructureRules.createsCycle(relation, existing)) throw new AcademicStructureConflictException();
        jdbcTemplate.update("""
                INSERT INTO academic_organization_relation (parent_unit_id, child_unit_id, valid_from, valid_through)
                VALUES (?, ?, ?, ?)
                """, relation.parentUnitId().toString(), relation.childUnitId().toString(),
                relation.validFrom(), relation.validThrough());
        audit(relation.childUnitId(), "UNIT_RELATED", actorSub, sourceReference,
                "Organization unit " + relation.childUnitId() + " added under " + relation.parentUnitId());
    }

    @Override
    @Transactional
    public void relateSites(AcademicSiteRelation relation, String actorSub, String sourceReference) {
        lockStructure();
        Validity parent = requireActiveSite(relation.parentSiteId());
        Validity child = requireActiveSite(relation.childSiteId());
        requireContained(relation.validFrom(), relation.validThrough(), parent);
        requireContained(relation.validFrom(), relation.validThrough(), child);
        List<AcademicSiteRelation> existing = allSiteRelations();
        ensureSingleParent(relation.childSiteId(), relation.validFrom(), relation.validThrough(),
                existing.stream().map(edge -> new ExistingRelation(edge.childSiteId(), edge.validFrom(), edge.validThrough())).toList());
        if (AcademicStructureRules.createsCycle(relation, existing)) throw new AcademicStructureConflictException();
        jdbcTemplate.update("""
                INSERT INTO academic_site_relation (parent_site_id, child_site_id, valid_from, valid_through)
                VALUES (?, ?, ?, ?)
                """, relation.parentSiteId().toString(), relation.childSiteId().toString(),
                relation.validFrom(), relation.validThrough());
        audit(relation.childSiteId(), "SITE_RELATED", actorSub, sourceReference,
                "Academic site " + relation.childSiteId() + " added under " + relation.parentSiteId());
    }

    @Override
    @Transactional
    public void affiliateProgram(AcademicProgramAffiliation affiliation, String actorSub) {
        lockStructure();
        if (!exists("SELECT COUNT(*) FROM academic_program WHERE program_id = ?", affiliation.programId())) {
            throw new AcademicStructureNotFoundException();
        }
        Validity unit = requireActiveUnit(affiliation.organizationUnitId());
        Validity site = requireActiveSite(affiliation.siteId());
        requireContained(affiliation.validFrom(), affiliation.validThrough(), unit);
        requireContained(affiliation.validFrom(), affiliation.validThrough(), site);
        List<ExistingRelation> existing = jdbcTemplate.query("""
                SELECT program_id AS child_id, valid_from, valid_through
                FROM academic_program_affiliation WHERE program_id = ?
                """, (rs, row) -> new ExistingRelation(uuid(rs.getString("child_id")),
                localDate(rs, "valid_from"), nullableDate(rs, "valid_through")), affiliation.programId().toString());
        ensureSingleParent(affiliation.programId(), affiliation.validFrom(), affiliation.validThrough(), existing);
        Instant now = clock.instant();
        jdbcTemplate.update("""
                INSERT INTO academic_program_affiliation
                    (affiliation_id, program_id, organization_unit_id, site_id, display_order, valid_from, valid_through,
                     source_reference, created_by, created_at)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                """, affiliation.id().toString(), affiliation.programId().toString(),
                affiliation.organizationUnitId().toString(), affiliation.siteId().toString(),
                affiliation.displayOrder(), affiliation.validFrom(), affiliation.validThrough(),
                affiliation.sourceReference(), actorSub,
                timestamp(now));
        audit(affiliation.id(), "PROGRAM_AFFILIATED", actorSub, affiliation.sourceReference(),
                "Academic program affiliated: " + affiliation.programId());
    }

    private void lockStructure() {
        jdbcTemplate.queryForObject("SELECT control_id FROM academic_structure_control WHERE control_id = 1 FOR UPDATE",
                Integer.class);
    }

    private Validity requireActiveUnit(UUID id) {
        List<Validity> values = jdbcTemplate.query("""
                SELECT status, valid_from, valid_through FROM academic_organization_unit WHERE organization_unit_id = ?
                """, (rs, row) -> new Validity(AcademicEntityStatus.valueOf(rs.getString("status")),
                localDate(rs, "valid_from"), nullableDate(rs, "valid_through")), id.toString());
        if (values.isEmpty()) throw new AcademicStructureNotFoundException();
        Validity validity = values.getFirst();
        if (validity.status() != AcademicEntityStatus.ACTIVE) throw new AcademicStructureConflictException();
        return validity;
    }

    private Validity requireActiveSite(UUID id) {
        List<Validity> values = jdbcTemplate.query("""
                SELECT status, valid_from, valid_through FROM academic_site WHERE site_id = ?
                """, (rs, row) -> new Validity(AcademicEntityStatus.valueOf(rs.getString("status")),
                localDate(rs, "valid_from"), nullableDate(rs, "valid_through")), id.toString());
        if (values.isEmpty()) throw new AcademicStructureNotFoundException();
        Validity validity = values.getFirst();
        if (validity.status() != AcademicEntityStatus.ACTIVE) throw new AcademicStructureConflictException();
        return validity;
    }

    private static void requireContained(LocalDate from, LocalDate through, Validity outer) {
        if (!AcademicStructureRules.containedBy(from, through, outer.validFrom(), outer.validThrough())) {
            throw new AcademicStructureConflictException();
        }
    }

    private List<AcademicOrganizationRelation> allOrganizationRelations() {
        return jdbcTemplate.query("""
                SELECT parent_unit_id, child_unit_id, valid_from, valid_through
                FROM academic_organization_relation ORDER BY parent_unit_id, child_unit_id, valid_from
                """, ORGANIZATION_RELATION_MAPPER);
    }

    private List<AcademicSiteRelation> allSiteRelations() {
        return jdbcTemplate.query("""
                SELECT parent_site_id, child_site_id, valid_from, valid_through
                FROM academic_site_relation ORDER BY parent_site_id, child_site_id, valid_from
                """, SITE_RELATION_MAPPER);
    }

    private static void ensureSingleParent(UUID childId, LocalDate from, LocalDate through,
                                          List<ExistingRelation> existing) {
        if (existing.stream().anyMatch(edge -> edge.childId().equals(childId)
                && AcademicStructureRules.overlaps(from, through, edge.validFrom(), edge.validThrough()))) {
            throw new AcademicStructureConflictException();
        }
    }

    private boolean exists(String sql, UUID id) {
        return jdbcTemplate.queryForObject(sql, Integer.class, id.toString()) > 0;
    }

    private void audit(UUID entityId, String action, String actorSub, String sourceReference, String summary) {
        jdbcTemplate.update("""
                INSERT INTO academic_structure_audit_event
                    (entity_id, action_key, actor_sub, source_reference, occurred_at, event_summary)
                VALUES (?, ?, ?, ?, ?, ?)
                """, entityId.toString(), action, actorSub, sourceReference,
                timestamp(clock.instant()), truncate(summary, 240));
    }

    private static String truncate(String value, int max) {
        return value.length() <= max ? value : value.substring(0, max);
    }

    private static UUID uuid(String value) {
        return value == null ? null : UUID.fromString(value.trim());
    }

    private static LocalDate localDate(ResultSet rs, String column) throws SQLException {
        return rs.getDate(column).toLocalDate();
    }

    private static LocalDate nullableDate(ResultSet rs, String column) throws SQLException {
        java.sql.Date value = rs.getDate(column);
        return value == null ? null : value.toLocalDate();
    }

    private static Timestamp timestamp(Instant value) {
        return Timestamp.from(value);
    }

    private record Validity(AcademicEntityStatus status, LocalDate validFrom, LocalDate validThrough) {
    }

    private record ExistingRelation(UUID childId, LocalDate validFrom, LocalDate validThrough) {
    }
}
