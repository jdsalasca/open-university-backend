package co.edu.uptc.universiry.notices.infrastructure.persistence;

import co.edu.uptc.universiry.notices.application.InstitutionalNoticeRepository;
import co.edu.uptc.universiry.notices.domain.InstitutionalNotice;
import co.edu.uptc.universiry.notices.domain.NoticeAudience;
import co.edu.uptc.universiry.notices.domain.NoticeAudienceKind;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowCallbackHandler;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Repository
public class JdbcInstitutionalNoticeRepositoryAdapter implements InstitutionalNoticeRepository {

    private static final String NOTICE_COLUMNS = """
            n.notice_id, n.title, n.body, n.source_reference,
            n.published_from, n.published_through, n.published_by, n.published_at,
            a.audience_kind, a.scope_reference
            """;

    // The page of notices is limited before the audiences are joined, so the SQL limit never truncates a notice.
    private static final String RECENT_SELECT = """
            SELECT %s
            FROM (
                SELECT notice_id, title, body, source_reference, published_from, published_through,
                       published_by, published_at
                FROM institutional_notice
                ORDER BY published_at DESC, notice_id
                LIMIT ?
            ) n
            LEFT JOIN institutional_notice_audience a ON a.notice_id = n.notice_id
            ORDER BY n.published_at DESC, n.notice_id, a.audience_kind, a.scope_reference
            """.formatted(NOTICE_COLUMNS);

    private static final String EFFECTIVE_SELECT_TEMPLATE = """
            SELECT %1$s
            FROM (
                SELECT notice_id, title, body, source_reference, published_from, published_through,
                       published_by, published_at
                FROM institutional_notice
                WHERE published_from <= ? AND published_through >= ?
                ORDER BY published_at DESC, notice_id
                LIMIT ?
            ) n
            JOIN institutional_notice_audience a ON a.notice_id = n.notice_id
            WHERE %2$s
            ORDER BY n.published_at DESC, n.notice_id, a.audience_kind, a.scope_reference
            """;

    private final JdbcTemplate jdbcTemplate;

    public JdbcInstitutionalNoticeRepositoryAdapter(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    @Transactional
    public void append(InstitutionalNotice notice) {
        jdbcTemplate.update("""
                INSERT INTO institutional_notice
                    (notice_id, title, body, source_reference, published_from, published_through,
                     published_by, published_at)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?)
                """,
                notice.noticeId(), notice.title(), notice.body(), notice.sourceReference(),
                notice.publishedFrom(), notice.publishedThrough(), notice.publishedBy(),
                Timestamp.from(notice.publishedAt()));

        for (NoticeAudience audience : notice.audiences()) {
            jdbcTemplate.update("""
                    INSERT INTO institutional_notice_audience (notice_id, audience_kind, scope_reference)
                    VALUES (?, ?, ?)
                    """,
                    notice.noticeId(), audience.kind().name(),
                    audience.reference() == null ? "" : audience.reference());
        }

        jdbcTemplate.update("""
                INSERT INTO institutional_notice_audit_event
                    (notice_id, action_key, actor_sub, occurred_at, source_reference, audience_count)
                VALUES (?, 'NOTICE_PUBLISHED', ?, ?, ?, ?)
                """,
                notice.noticeId(), notice.publishedBy(), Timestamp.from(notice.publishedAt()),
                notice.sourceReference(), notice.audiences().size());
    }

    @Override
    public List<InstitutionalNotice> recent(int limit) {
        return read(RECENT_SELECT, List.of(limit));
    }

    @Override
    public List<InstitutionalNotice> effectiveOn(LocalDate date, List<NoticeAudience> audiences, int limit) {
        StringBuilder matchingAudience = new StringBuilder("a.audience_kind = 'UNIVERSITY'");
        List<Object> arguments = new ArrayList<>();
        arguments.add(date);
        arguments.add(date);
        for (NoticeAudience audience : audiences) {
            if (audience.kind() == NoticeAudienceKind.UNIVERSITY) {
                // A university-wide assignment already matches every notice, so no extra predicate is needed.
                continue;
            }
            matchingAudience.append(" OR (a.audience_kind = ? AND a.scope_reference = ?)");
            arguments.add(audience.kind().name());
            arguments.add(audience.reference());
        }
        arguments.add(limit);
        return read(EFFECTIVE_SELECT_TEMPLATE.formatted(NOTICE_COLUMNS, matchingAudience), arguments);
    }

    private List<InstitutionalNotice> read(String sql, List<Object> arguments) {
        Map<String, NoticeAccumulator> notices = new LinkedHashMap<>();
        RowCallbackHandler collector = rs -> {
            String noticeId = rs.getString("notice_id");
            NoticeAccumulator notice = notices.get(noticeId);
            if (notice == null) {
                notice = new NoticeAccumulator(rs);
                notices.put(noticeId, notice);
            }
            String kind = rs.getString("audience_kind");
            if (kind != null) {
                String reference = rs.getString("scope_reference");
                notice.audiences.add(new NoticeAudience(
                        NoticeAudienceKind.valueOf(kind), reference.isEmpty() ? null : reference));
            }
        };
        jdbcTemplate.query(sql, collector, arguments.toArray());
        return notices.values().stream().map(NoticeAccumulator::toNotice).toList();
    }

    private static final class NoticeAccumulator {
        private final String noticeId;
        private final String title;
        private final String body;
        private final String sourceReference;
        private final LocalDate publishedFrom;
        private final LocalDate publishedThrough;
        private final String publishedBy;
        private final Instant publishedAt;
        private final List<NoticeAudience> audiences = new ArrayList<>();

        private NoticeAccumulator(ResultSet rs) throws SQLException {
            noticeId = rs.getString("notice_id");
            title = rs.getString("title");
            body = rs.getString("body");
            sourceReference = rs.getString("source_reference");
            publishedFrom = rs.getDate("published_from").toLocalDate();
            publishedThrough = rs.getDate("published_through").toLocalDate();
            publishedBy = rs.getString("published_by");
            publishedAt = rs.getTimestamp("published_at").toInstant();
        }

        private InstitutionalNotice toNotice() {
            return new InstitutionalNotice(noticeId, title, body, sourceReference,
                    publishedFrom, publishedThrough, audiences, publishedBy, publishedAt);
        }
    }
}