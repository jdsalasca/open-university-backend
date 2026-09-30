package co.edu.uptc.universiry.academics.infrastructure.persistence;

import co.edu.uptc.universiry.academics.application.AcademicCatalogRepository;
import co.edu.uptc.universiry.academics.application.AcademicCatalogActorSub;
import co.edu.uptc.universiry.academics.application.AcademicCurriculumDetails;
import co.edu.uptc.universiry.academics.application.AcademicCurriculumDraftsPage;
import co.edu.uptc.universiry.academics.application.AcademicCurriculumEntriesPage;
import co.edu.uptc.universiry.academics.application.AcademicCurriculumEntrySummary;
import co.edu.uptc.universiry.academics.application.AcademicProgramSummary;
import co.edu.uptc.universiry.academics.application.CurriculumEntriesPageQuery;
import co.edu.uptc.universiry.academics.application.CurriculumDraftCursor;
import co.edu.uptc.universiry.academics.application.CurriculumDraftsPageQuery;
import co.edu.uptc.universiry.academics.application.CurriculumPublishResult;
import co.edu.uptc.universiry.academics.application.CurriculumSummary;
import co.edu.uptc.universiry.academics.application.CurriculumVersionConflictException;
import co.edu.uptc.universiry.academics.application.ValidatedCurriculum;
import co.edu.uptc.universiry.academics.application.ValidatedCurriculumEntry;
import co.edu.uptc.universiry.academics.application.ValidatedProgram;
import co.edu.uptc.universiry.academics.domain.AcademicCurriculumStatus;
import co.edu.uptc.universiry.academics.domain.AcademicLevel;
import co.edu.uptc.universiry.academics.domain.StudyModality;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.regex.Pattern;

@Repository
public class JdbcAcademicCatalogRepositoryAdapter implements AcademicCatalogRepository {

    private static final Pattern SOURCE_HASH = Pattern.compile("^[0-9a-f]{64}$");

    private static final String SUMMARY_SELECT = """
            SELECT c.curriculum_id, c.program_id, p.program_code, p.academic_level, p.study_modality,
                   p.campus_code, pr.program_name, pr.faculty, pr.campus_name, c.curriculum_version,
                   c.cohort_from, c.cohort_through, c.approval_reference, c.status,
                   (SELECT COUNT(*) FROM academic_curriculum_entry e WHERE e.curriculum_id = c.curriculum_id)
                       AS entry_count,
                   c.source_sha256, c.created_at, c.created_by, c.published_by, c.published_at
            FROM academic_curriculum c
            JOIN academic_program p ON p.program_id = c.program_id
            JOIN academic_program_revision pr
              ON pr.program_revision_id = c.program_revision_id AND pr.program_id = c.program_id
            """;

    private static final RowMapper<CurriculumSummary> CURRICULUM_SUMMARY_MAPPER = (resultSet, rowNumber) ->
            new CurriculumSummary(
                    uuid(resultSet.getString("curriculum_id")),
                    uuid(resultSet.getString("program_id")),
                    resultSet.getString("program_code").trim(),
                    AcademicLevel.valueOf(resultSet.getString("academic_level")),
                    StudyModality.valueOf(resultSet.getString("study_modality")),
                    resultSet.getString("campus_code").trim(),
                    resultSet.getString("program_name"),
                    resultSet.getString("faculty"),
                    resultSet.getString("campus_name"),
                    resultSet.getString("curriculum_version"),
                    resultSet.getString("cohort_from").trim(),
                    resultSet.getString("cohort_through") == null ? null : resultSet.getString("cohort_through").trim(),
                    resultSet.getString("approval_reference"),
                    AcademicCurriculumStatus.valueOf(resultSet.getString("status")),
                    resultSet.getInt("entry_count"),
                    resultSet.getString("source_sha256").trim(),
                    instant(resultSet, "created_at"),
                    resultSet.getString("created_by"),
                    resultSet.getString("published_by"),
                    instant(resultSet, "published_at"));

    private static final RowMapper<AcademicCurriculumEntrySummary> CURRICULUM_ENTRY_SUMMARY_MAPPER =
            (resultSet, rowNumber) -> new AcademicCurriculumEntrySummary(
                    uuid(resultSet.getString("subject_id")),
                    uuid(resultSet.getString("subject_revision_id")),
                    resultSet.getString("subject_code").trim(),
                    resultSet.getString("subject_name"),
                    resultSet.getBigDecimal("credits"),
                    resultSet.getInt("semester"),
                    resultSet.getString("formation_space"),
                    resultSet.getString("component"),
                    resultSet.getString("choice_group"),
                    resultSet.getInt("row_order"));

    private final JdbcTemplate jdbcTemplate;

    public JdbcAcademicCatalogRepositoryAdapter(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    @Transactional
    public CurriculumSummary createDraft(ValidatedCurriculum curriculum, String actorSub) {
        Objects.requireNonNull(curriculum, "curriculum");
        String actor = AcademicCatalogActorSub.require(actorSub);
        String sourceHash = curriculum.sourceSha256();
        if (sourceHash == null || !SOURCE_HASH.matcher(sourceHash).matches()) {
            throw new IllegalArgumentException("The source hash is invalid.");
        }

        LocalDateTime now = LocalDateTime.now(ZoneOffset.UTC);
        ValidatedProgram sourceProgram = curriculum.program();
        UUID programId = findOrCreateProgram(sourceProgram, now);
        UUID programRevisionId = findOrCreateProgramRevision(programId, sourceProgram, now);

        List<EntryReference> entryReferences = new ArrayList<>(curriculum.entries().size());
        for (ValidatedCurriculumEntry entry : curriculum.entries()) {
            UUID subjectId = findOrCreateSubject(entry.subjectCode(), now);
            UUID subjectRevisionId = findOrCreateSubjectRevision(subjectId, entry, now);
            entryReferences.add(new EntryReference(subjectId, subjectRevisionId, entry));
        }

        UUID curriculumId = UUID.randomUUID();
        try {
            jdbcTemplate.update("""
                    INSERT INTO academic_curriculum (
                        curriculum_id, program_id, program_revision_id, curriculum_version,
                        cohort_from, cohort_through, approval_reference, status, source_sha256,
                        created_by, created_at, published_by, published_at
                    ) VALUES (?, ?, ?, ?, ?, ?, ?, 'DRAFT', ?, ?, ?, NULL, NULL)
                    """,
                    curriculumId.toString(), programId.toString(), programRevisionId.toString(),
                    curriculum.curriculumVersion(), curriculum.cohortFrom(), curriculum.cohortThrough(),
                    curriculum.approvalReference(), sourceHash, actor, now);
        } catch (DuplicateKeyException conflict) {
            throw new CurriculumVersionConflictException();
        }

        insertEntries(curriculumId, entryReferences);
        insertAuditEvent(curriculumId, actor, "CURRICULUM_IMPORTED", sourceHash, now,
                "Versioned curriculum import created a draft.");
        return findCurriculum(curriculumId)
                .map(AcademicCurriculumDetails::curriculum)
                .orElseThrow(() -> new IllegalStateException("The created curriculum could not be read."));
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<AcademicCurriculumDetails> findCurriculum(UUID curriculumId) {
        if (curriculumId == null) {
            return Optional.empty();
        }
        List<CurriculumSummary> summaries = jdbcTemplate.query(
                SUMMARY_SELECT + " WHERE c.curriculum_id = ?",
                CURRICULUM_SUMMARY_MAPPER,
                curriculumId.toString());
        if (summaries.isEmpty()) {
            return Optional.empty();
        }

        List<AcademicCurriculumEntrySummary> entries = jdbcTemplate.query("""
                SELECT e.subject_id, e.subject_revision_id, s.subject_code, sr.subject_name, sr.credits,
                       e.semester, e.formation_space, e.component, e.choice_group, e.row_order
                FROM academic_curriculum_entry e
                JOIN academic_subject s ON s.subject_id = e.subject_id
                JOIN academic_subject_revision sr
                  ON sr.subject_revision_id = e.subject_revision_id AND sr.subject_id = e.subject_id
                WHERE e.curriculum_id = ?
                ORDER BY e.row_order
                """,
                CURRICULUM_ENTRY_SUMMARY_MAPPER,
                curriculumId.toString());
        return Optional.of(new AcademicCurriculumDetails(summaries.getFirst(), entries));
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<CurriculumSummary> findPublishedCurriculumSummary(UUID curriculumId) {
        if (curriculumId == null) {
            return Optional.empty();
        }
        return jdbcTemplate.query(
                        SUMMARY_SELECT + " WHERE c.curriculum_id = ? AND c.status = 'PUBLISHED'",
                        CURRICULUM_SUMMARY_MAPPER,
                        curriculumId.toString())
                .stream()
                .findFirst();
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<AcademicCurriculumEntriesPage> findPublishedCurriculumEntries(
            UUID curriculumId,
            CurriculumEntriesPageQuery query
    ) {
        Objects.requireNonNull(query, "query");
        if (curriculumId == null) {
            return Optional.empty();
        }

        EntryFilters filters = entryFilters(query);
        String countSql = """
                SELECT COUNT(e.entry_id) AS total_items
                FROM academic_curriculum c
                LEFT JOIN academic_curriculum_entry e
                  ON e.curriculum_id = c.curriculum_id AND %s
                WHERE c.curriculum_id = ? AND c.status = 'PUBLISHED'
                GROUP BY c.curriculum_id
                """.formatted(filters.sql());
        List<Object> countParameters = new ArrayList<>(filters.parameters());
        countParameters.add(curriculumId.toString());
        List<Long> counts = jdbcTemplate.query(
                countSql,
                (resultSet, rowNumber) -> resultSet.getLong("total_items"),
                countParameters.toArray());
        if (counts.isEmpty()) {
            return Optional.empty();
        }

        long totalItems = counts.getFirst();
        int totalPages = totalItems == 0
                ? 0
                : 1 + (int) ((totalItems - 1) / query.pageSize());
        long offset = ((long) query.page() - 1L) * query.pageSize();
        List<AcademicCurriculumEntrySummary> entries = List.of();
        if (offset < totalItems) {
            String pageSql = """
                    SELECT e.subject_id, e.subject_revision_id, s.subject_code, sr.subject_name, sr.credits,
                           e.semester, e.formation_space, e.component, e.choice_group, e.row_order
                    FROM academic_curriculum c
                    JOIN academic_curriculum_entry e ON e.curriculum_id = c.curriculum_id
                    JOIN academic_subject s ON s.subject_id = e.subject_id
                    JOIN academic_subject_revision sr
                      ON sr.subject_revision_id = e.subject_revision_id AND sr.subject_id = e.subject_id
                    WHERE c.curriculum_id = ? AND c.status = 'PUBLISHED' AND %s
                    ORDER BY e.semester, e.row_order
                    LIMIT ? OFFSET ?
                    """.formatted(filters.sql());
            List<Object> pageParameters = new ArrayList<>();
            pageParameters.add(curriculumId.toString());
            pageParameters.addAll(filters.parameters());
            pageParameters.add(query.pageSize());
            pageParameters.add(offset);
            entries = jdbcTemplate.query(pageSql, CURRICULUM_ENTRY_SUMMARY_MAPPER, pageParameters.toArray());
        }

        return Optional.of(new AcademicCurriculumEntriesPage(
                curriculumId, query.page(), query.pageSize(), totalItems, totalPages, entries));
    }

    private static EntryFilters entryFilters(CurriculumEntriesPageQuery query) {
        StringBuilder sql = new StringBuilder();
        List<Object> parameters = new ArrayList<>();
        if (query.semester() != null) {
            sql.append("e.semester = ?");
            parameters.add(query.semester());
        }
        if (query.search() != null) {
            if (!sql.isEmpty()) {
                sql.append(" AND ");
            }
            sql.append("(LOWER(e.search_subject_code) LIKE LOWER(?) ESCAPE '!' "
                    + "OR LOWER(e.search_subject_name) LIKE LOWER(?) ESCAPE '!')");
            String searchPattern = "%" + query.search()
                    .replace("!", "!!")
                    .replace("%", "!%")
                    .replace("_", "!_") + "%";
            parameters.add(searchPattern);
            parameters.add(searchPattern);
        }
        return new EntryFilters(sql.isEmpty() ? "1 = 1" : sql.toString(), List.copyOf(parameters));
    }

    @Override
    @Transactional(readOnly = true)
    public List<AcademicProgramSummary> listPublishedPrograms() {
        return jdbcTemplate.query("""
                SELECT p.program_id, p.program_code, p.academic_level, p.study_modality, p.campus_code,
                       pr.program_name, pr.faculty, pr.campus_name
                FROM academic_program p
                JOIN academic_curriculum c
                  ON c.program_id = p.program_id AND c.status = 'PUBLISHED'
                JOIN academic_program_revision pr
                  ON pr.program_revision_id = c.program_revision_id AND pr.program_id = c.program_id
                WHERE NOT EXISTS (
                    SELECT 1
                    FROM academic_curriculum newer
                    WHERE newer.program_id = c.program_id
                      AND newer.status = 'PUBLISHED'
                      AND (newer.created_at > c.created_at
                           OR (newer.created_at = c.created_at AND newer.curriculum_id > c.curriculum_id))
                )
                ORDER BY p.program_code, p.campus_code
                """,
                (resultSet, rowNumber) -> new AcademicProgramSummary(
                        uuid(resultSet.getString("program_id")),
                        resultSet.getString("program_code").trim(),
                        AcademicLevel.valueOf(resultSet.getString("academic_level")),
                        StudyModality.valueOf(resultSet.getString("study_modality")),
                        resultSet.getString("campus_code").trim(),
                        resultSet.getString("program_name"),
                        resultSet.getString("faculty"),
                        resultSet.getString("campus_name")));
    }

    @Override
    @Transactional(readOnly = true)
    public List<CurriculumSummary> listPublishedCurricula(UUID programId) {
        if (programId == null) {
            return List.of();
        }
        return jdbcTemplate.query(
                SUMMARY_SELECT + " WHERE c.program_id = ? AND c.status = 'PUBLISHED' "
                        + "ORDER BY c.cohort_from, c.curriculum_version, c.created_at, c.curriculum_id",
                CURRICULUM_SUMMARY_MAPPER,
                programId.toString());
    }

    @Override
    @Transactional(readOnly = true)
    public AcademicCurriculumDraftsPage listDrafts(CurriculumDraftsPageQuery query) {
        long totalItems = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM academic_curriculum WHERE status = 'DRAFT'", Long.class);
        List<Object> parameters = new ArrayList<>();
        String cursorFilter = "";
        if (query.after() != null) {
            LocalDateTime cursorCreatedAt = query.after().createdAt();
            cursorFilter = " AND (draft.created_at < ? OR "
                    + "(draft.created_at = ? AND draft.curriculum_id < ?)) ";
            parameters.add(cursorCreatedAt);
            parameters.add(cursorCreatedAt);
            parameters.add(query.after().curriculumId().toString());
        }
        parameters.add(query.pageSize() + 1);
        String draftsPageSql = """
                SELECT c.curriculum_id, c.program_id, p.program_code, p.academic_level, p.study_modality,
                       p.campus_code, pr.program_name, pr.faculty, pr.campus_name, c.curriculum_version,
                       c.cohort_from, c.cohort_through, c.approval_reference, c.status,
                       (SELECT COUNT(*) FROM academic_curriculum_entry e WHERE e.curriculum_id = c.curriculum_id)
                           AS entry_count,
                       c.source_sha256, c.created_at, c.created_by, c.published_by, c.published_at
                FROM (
                    SELECT draft.curriculum_id, draft.program_id, draft.program_revision_id,
                           draft.curriculum_version, draft.cohort_from, draft.cohort_through,
                           draft.approval_reference, draft.status, draft.source_sha256,
                           draft.created_by, draft.created_at, draft.published_by, draft.published_at
                    FROM academic_curriculum draft
                    WHERE draft.status = 'DRAFT' %s
                    ORDER BY draft.created_at DESC, draft.curriculum_id DESC
                    LIMIT ?
                ) c
                JOIN academic_program p ON p.program_id = c.program_id
                JOIN academic_program_revision pr
                  ON pr.program_revision_id = c.program_revision_id AND pr.program_id = c.program_id
                ORDER BY c.created_at DESC, c.curriculum_id DESC
                """.formatted(cursorFilter);
        List<CurriculumSummary> candidates = jdbcTemplate.query(
                draftsPageSql,
                CURRICULUM_SUMMARY_MAPPER,
                parameters.toArray());
        boolean hasNext = candidates.size() > query.pageSize();
        List<CurriculumSummary> drafts = List.copyOf(candidates.subList(
                0, Math.min(candidates.size(), query.pageSize())));
        CurriculumDraftCursor nextCursor = hasNext
                ? new CurriculumDraftCursor(
                        LocalDateTime.ofInstant(drafts.getLast().createdAt(), ZoneOffset.UTC), drafts.getLast().id())
                : null;
        return new AcademicCurriculumDraftsPage(
                query.pageSize(), totalItems, drafts, nextCursor);
    }

    @Override
    @Transactional
    public CurriculumPublishResult publishDraft(UUID curriculumId, String actorSub) {
        if (curriculumId == null) {
            return CurriculumPublishResult.NOT_FOUND;
        }
        String actor = AcademicCatalogActorSub.require(actorSub);
        LocalDateTime now = LocalDateTime.now(ZoneOffset.UTC);
        int changed = jdbcTemplate.update("""
                UPDATE academic_curriculum
                SET status = 'PUBLISHED', published_by = ?, published_at = ?
                WHERE curriculum_id = ? AND status = 'DRAFT'
                """,
                actor, now, curriculumId.toString());
        if (changed == 1) {
            String sourceHash = sourceHash(curriculumId).orElseThrow(
                    () -> new IllegalStateException("The published curriculum disappeared."));
            insertAuditEvent(curriculumId, actor, "CURRICULUM_PUBLISHED", sourceHash, now,
                    "Curriculum draft published.");
            return CurriculumPublishResult.PUBLISHED;
        }
        return curriculumStatus(curriculumId)
                .map(ignored -> CurriculumPublishResult.CONFLICT)
                .orElse(CurriculumPublishResult.NOT_FOUND);
    }

    private UUID findOrCreateProgram(ValidatedProgram program, LocalDateTime now) {
        Optional<UUID> existing = findProgram(program);
        if (existing.isPresent()) {
            return existing.get();
        }
        UUID candidate = UUID.randomUUID();
        try {
            jdbcTemplate.update("""
                    INSERT INTO academic_program (
                        program_id, program_code, academic_level, study_modality, campus_code, created_at
                    ) VALUES (?, ?, ?, ?, ?, ?)
                    """,
                    candidate.toString(), program.programCode(), program.academicLevel().name(),
                    program.studyModality().name(), program.campusCode(), now);
        } catch (DuplicateKeyException concurrentCreation) {
            // A concurrent import may have inserted the same stable program identity.
        }
        return findProgram(program).orElseThrow(
                () -> new IllegalStateException("The academic program could not be created or reused."));
    }

    private Optional<UUID> findProgram(ValidatedProgram program) {
        List<UUID> ids = jdbcTemplate.query("""
                SELECT program_id FROM academic_program
                WHERE program_code = ? AND academic_level = ? AND study_modality = ? AND campus_code = ?
                """,
                (resultSet, rowNumber) -> uuid(resultSet.getString("program_id")),
                program.programCode(), program.academicLevel().name(),
                program.studyModality().name(), program.campusCode());
        return ids.stream().findFirst();
    }

    private UUID findOrCreateProgramRevision(UUID programId, ValidatedProgram program, LocalDateTime now) {
        String fingerprint = fingerprint(
                "academic-program-revision-v1", program.sniesCode(), program.programName(),
                program.faculty(), program.campusName());
        Optional<UUID> existing = findProgramRevision(programId, fingerprint);
        if (existing.isPresent()) {
            return existing.get();
        }
        UUID candidate = UUID.randomUUID();
        try {
            jdbcTemplate.update("""
                    INSERT INTO academic_program_revision (
                        program_revision_id, program_id, content_fingerprint, snies_code,
                        program_name, faculty, campus_name, created_at
                    ) VALUES (?, ?, ?, ?, ?, ?, ?, ?)
                    """,
                    candidate.toString(), programId.toString(), fingerprint, program.sniesCode(),
                    program.programName(), program.faculty(), program.campusName(), now);
        } catch (DuplicateKeyException concurrentCreation) {
            // The unique fingerprint resolves concurrent creation of identical display metadata.
        }
        return findProgramRevision(programId, fingerprint).orElseThrow(
                () -> new IllegalStateException("The academic program revision could not be reused."));
    }

    private Optional<UUID> findProgramRevision(UUID programId, String fingerprint) {
        List<UUID> ids = jdbcTemplate.query("""
                SELECT program_revision_id FROM academic_program_revision
                WHERE program_id = ? AND content_fingerprint = ?
                """,
                (resultSet, rowNumber) -> uuid(resultSet.getString("program_revision_id")),
                programId.toString(), fingerprint);
        return ids.stream().findFirst();
    }

    private UUID findOrCreateSubject(String subjectCode, LocalDateTime now) {
        Optional<UUID> existing = findSubject(subjectCode);
        if (existing.isPresent()) {
            return existing.get();
        }
        UUID candidate = UUID.randomUUID();
        try {
            jdbcTemplate.update(
                    "INSERT INTO academic_subject (subject_id, subject_code, created_at) VALUES (?, ?, ?)",
                    candidate.toString(), subjectCode, now);
        } catch (DuplicateKeyException concurrentCreation) {
            // The unique subject code resolves concurrent creation of the same stable subject.
        }
        return findSubject(subjectCode).orElseThrow(
                () -> new IllegalStateException("The academic subject could not be created or reused."));
    }

    private Optional<UUID> findSubject(String subjectCode) {
        List<UUID> ids = jdbcTemplate.query(
                "SELECT subject_id FROM academic_subject WHERE subject_code = ?",
                (resultSet, rowNumber) -> uuid(resultSet.getString("subject_id")),
                subjectCode);
        return ids.stream().findFirst();
    }

    private UUID findOrCreateSubjectRevision(UUID subjectId, ValidatedCurriculumEntry entry, LocalDateTime now) {
        String fingerprint = fingerprint(
                "academic-subject-revision-v1", entry.subjectName(), entry.credits().toPlainString());
        Optional<UUID> existing = findSubjectRevision(subjectId, fingerprint);
        if (existing.isPresent()) {
            return existing.get();
        }
        UUID candidate = UUID.randomUUID();
        try {
            jdbcTemplate.update("""
                    INSERT INTO academic_subject_revision (
                        subject_revision_id, subject_id, content_fingerprint,
                        subject_name, credits, created_at
                    ) VALUES (?, ?, ?, ?, ?, ?)
                    """,
                    candidate.toString(), subjectId.toString(), fingerprint,
                    entry.subjectName(), entry.credits(), now);
        } catch (DuplicateKeyException concurrentCreation) {
            // The unique fingerprint resolves concurrent creation of identical subject content.
        }
        return findSubjectRevision(subjectId, fingerprint).orElseThrow(
                () -> new IllegalStateException("The academic subject revision could not be reused."));
    }

    private Optional<UUID> findSubjectRevision(UUID subjectId, String fingerprint) {
        List<UUID> ids = jdbcTemplate.query("""
                SELECT subject_revision_id FROM academic_subject_revision
                WHERE subject_id = ? AND content_fingerprint = ?
                """,
                (resultSet, rowNumber) -> uuid(resultSet.getString("subject_revision_id")),
                subjectId.toString(), fingerprint);
        return ids.stream().findFirst();
    }

    private void insertEntries(UUID curriculumId, List<EntryReference> entries) {
        jdbcTemplate.batchUpdate("""
                INSERT INTO academic_curriculum_entry (
                    curriculum_id, subject_id, subject_revision_id, semester,
                    formation_space, component, choice_group, row_order,
                    search_subject_code, search_subject_name
                ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                """,
                entries,
                500,
                (PreparedStatement statement, EntryReference reference) -> bindEntry(statement, curriculumId, reference));
    }

    private static void bindEntry(PreparedStatement statement, UUID curriculumId, EntryReference reference)
            throws SQLException {
        ValidatedCurriculumEntry entry = reference.source();
        statement.setString(1, curriculumId.toString());
        statement.setString(2, reference.subjectId().toString());
        statement.setString(3, reference.subjectRevisionId().toString());
        statement.setInt(4, entry.semester());
        statement.setString(5, entry.formationSpace());
        statement.setString(6, entry.component());
        statement.setString(7, entry.choiceGroup());
        statement.setInt(8, entry.rowOrder());
        statement.setString(9, entry.subjectCode());
        statement.setString(10, entry.subjectName());
    }

    private void insertAuditEvent(
            UUID curriculumId,
            String actorSub,
            String actionKey,
            String sourceHash,
            LocalDateTime occurredAt,
            String summary
    ) {
        jdbcTemplate.update("""
                INSERT INTO academic_catalog_audit_event (
                    curriculum_id, actor_sub, action_key, source_sha256, occurred_at, event_summary
                ) VALUES (?, ?, ?, ?, ?, ?)
                """,
                curriculumId.toString(), actorSub, actionKey, sourceHash, occurredAt, summary);
    }

    private Optional<String> sourceHash(UUID curriculumId) {
        List<String> values = jdbcTemplate.query(
                "SELECT source_sha256 FROM academic_curriculum WHERE curriculum_id = ?",
                (resultSet, rowNumber) -> resultSet.getString("source_sha256").trim(),
                curriculumId.toString());
        return values.stream().findFirst();
    }

    private Optional<String> curriculumStatus(UUID curriculumId) {
        List<String> statuses = jdbcTemplate.query(
                "SELECT status FROM academic_curriculum WHERE curriculum_id = ?",
                (resultSet, rowNumber) -> resultSet.getString("status"),
                curriculumId.toString());
        return statuses.stream().findFirst();
    }

    private static String fingerprint(String... fields) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            for (String field : fields) {
                byte[] value = field == null ? null : field.getBytes(StandardCharsets.UTF_8);
                digest.update(ByteBuffer.allocate(Integer.BYTES)
                        .putInt(value == null ? -1 : value.length)
                        .array());
                if (value != null) {
                    digest.update(value);
                }
            }
            return HexFormat.of().formatHex(digest.digest());
        } catch (NoSuchAlgorithmException unavailable) {
            throw new IllegalStateException("SHA-256 is not available.");
        }
    }

    private static UUID uuid(String value) {
        return UUID.fromString(value.trim());
    }

    private static Instant instant(java.sql.ResultSet resultSet, String column) throws SQLException {
        LocalDateTime timestamp = resultSet.getObject(column, LocalDateTime.class);
        return timestamp == null ? null : timestamp.toInstant(ZoneOffset.UTC);
    }

    private record EntryReference(UUID subjectId, UUID subjectRevisionId, ValidatedCurriculumEntry source) {
    }

    private record EntryFilters(String sql, List<Object> parameters) {
    }
}
