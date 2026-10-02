package co.edu.uptc.universiry.admissions.domain;

import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

class AdmissionsCallRevisionTest {

    @Test
    void draft_rejects_partial_publication_metadata() {
        // Arrange
        UUID partialPublisher = UUID.randomUUID();

        // Act + Assert
        assertThrows(IllegalArgumentException.class, () -> revision(
                AdmissionsCallStatus.DRAFT, partialPublisher, null, null, null));
    }

    @Test
    void draft_without_publication_metadata_is_valid() {
        // Arrange + Act + Assert
        assertDoesNotThrow(() -> revision(AdmissionsCallStatus.DRAFT, null, null, null, null));
    }

    private static AdmissionsCallRevision revision(
            AdmissionsCallStatus status,
            UUID publishedByUserId,
            UUID publishedByIdentityId,
            Instant publishedAt,
            String officialReference
    ) {
        UUID revisionId = UUID.randomUUID();
        return new AdmissionsCallRevision(revisionId, UUID.randomUUID(), 1, 1, status, content(),
                UUID.randomUUID(), UUID.randomUUID(), Instant.parse("2026-10-01T12:00:00Z"),
                publishedByUserId, publishedByIdentityId, publishedAt, officialReference);
    }

    private static AdmissionsCallContent content() {
        AdmissionsMilestone milestone = new AdmissionsMilestone("registration", AdmissionsMilestoneKind.APPLICATION,
                LocalDate.parse("2027-01-01"), LocalDate.parse("2027-01-05"), "Registration", "Published window");
        AdmissionsSource source = new AdmissionsSource("Source", "https://example.edu/calendar");
        return new AdmissionsCallContent("Call title", "Call name", LocalDate.parse("2026-09-30"),
                LocalDate.parse("2026-10-01"), source, source, List.of(milestone));
    }
}
