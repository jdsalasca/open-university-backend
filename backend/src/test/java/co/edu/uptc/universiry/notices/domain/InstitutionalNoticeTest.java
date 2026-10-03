package co.edu.uptc.universiry.notices.domain;

import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class InstitutionalNoticeTest {

    private static final LocalDate FROM = LocalDate.parse("2026-10-03");
    private static final LocalDate THROUGH = LocalDate.parse("2026-10-10");

    @Test
    void a_notice_keeps_every_audience_selection_distinct_and_ordered() {
        // Arrange + Act
        var notice = notice(List.of(
                new NoticeAudience(NoticeAudienceKind.PROGRAM, "ING-SIS"),
                new NoticeAudience(NoticeAudienceKind.UNIVERSITY, null)));

        // Assert
        assertEquals(List.of(
                new NoticeAudience(NoticeAudienceKind.UNIVERSITY, null),
                new NoticeAudience(NoticeAudienceKind.PROGRAM, "ING-SIS")), notice.audiences());
    }

    @Test
    void a_university_wide_notice_rejects_a_reference_because_it_has_no_scope() {
        // Arrange + Act + Assert
        assertThrows(IllegalArgumentException.class,
                () -> new NoticeAudience(NoticeAudienceKind.UNIVERSITY, "TUNJA"));
    }

    @Test
    void a_scoped_notice_requires_a_non_blank_reference() {
        // Arrange + Act + Assert
        assertThrows(IllegalArgumentException.class, () -> new NoticeAudience(NoticeAudienceKind.FACULTY, "  "));
        assertThrows(IllegalArgumentException.class, () -> new NoticeAudience(NoticeAudienceKind.SITE, null));
    }

    @Test
    void a_notice_requires_at_least_one_audience_and_rejects_duplicates() {
        // Arrange + Act + Assert
        assertThrows(IllegalArgumentException.class, () -> notice(List.of()));
        assertThrows(IllegalArgumentException.class, () -> notice(List.of(
                new NoticeAudience(NoticeAudienceKind.SITE, "TUNJA"),
                new NoticeAudience(NoticeAudienceKind.SITE, "TUNJA"))));
    }

    @Test
    void a_notice_rejects_an_inverted_availability_window() {
        // Arrange + Act + Assert
        assertThrows(IllegalArgumentException.class,
                () -> new InstitutionalNotice("id", "title", "body", "reference",
                        LocalDate.parse("2026-10-10"), LocalDate.parse("2026-10-03"),
                        List.of(new NoticeAudience(NoticeAudienceKind.UNIVERSITY, null)), "actor", null));
    }

    @Test
    void a_notice_requires_title_body_and_reference() {
        // Arrange + Act + Assert
        assertThrows(IllegalArgumentException.class, () -> new InstitutionalNotice(
                "id", " ", "body", "reference", FROM, THROUGH, audiences(), "actor", null));
        assertThrows(IllegalArgumentException.class, () -> new InstitutionalNotice(
                "id", "title", " ", "reference", FROM, THROUGH, audiences(), "actor", null));
        assertThrows(IllegalArgumentException.class, () -> new InstitutionalNotice(
                "id", "title", "body", " ", FROM, THROUGH, audiences(), "actor", null));
        assertThrows(IllegalArgumentException.class, () -> new InstitutionalNotice(
                "id", "title", "body", "reference", null, THROUGH, audiences(), "actor", null));
        assertThrows(IllegalArgumentException.class, () -> new InstitutionalNotice(
                "id", "title", "body", "reference", FROM, THROUGH, audiences(), " ", null));
    }

    @Test
    void a_notice_accepts_a_closed_availability_window_on_a_single_day() {
        // Arrange + Act + Assert
        assertDoesNotThrow(() -> new InstitutionalNotice("id", "title", "body", "reference",
                FROM, FROM, audiences(), "actor", null));
    }

    private static InstitutionalNotice notice(List<NoticeAudience> audiences) {
        return new InstitutionalNotice("id", "title", "body", "reference", FROM, THROUGH, audiences, "actor", null);
    }

    private static List<NoticeAudience> audiences() {
        return List.of(new NoticeAudience(NoticeAudienceKind.UNIVERSITY, null));
    }
}