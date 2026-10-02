package co.edu.uptc.universiry.admissions.domain;

import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class AdmissionsCallContentTest {

    @Test
    void accepts_the_fifty_milestone_limit_and_sorts_by_start_date() {
        // Arrange
        List<AdmissionsMilestone> milestones = new ArrayList<>();
        for (int index = 49; index >= 0; index--) {
            LocalDate date = LocalDate.parse("2028-01-01").plusDays(index);
            milestones.add(milestone(index, date));
        }

        // Act
        AdmissionsCallContent content = content(milestones);

        // Assert
        assertEquals(50, content.milestones().size());
        assertEquals(LocalDate.parse("2028-01-01"), content.milestones().getFirst().startsOn());
        assertThrows(UnsupportedOperationException.class,
                () -> content.milestones().add(milestone(51, LocalDate.parse("2028-03-01"))));
    }

    @Test
    void rejects_more_than_fifty_milestones_and_duplicate_keys() {
        // Arrange
        List<AdmissionsMilestone> tooMany = new ArrayList<>();
        for (int index = 0; index < 51; index++) {
            tooMany.add(milestone(index, LocalDate.parse("2028-01-01").plusDays(index)));
        }
        List<AdmissionsMilestone> duplicateKeys = List.of(
                milestone(1, LocalDate.parse("2028-01-01")),
                milestone(1, LocalDate.parse("2028-01-02")));

        // Act + Assert
        assertThrows(IllegalArgumentException.class, () -> content(tooMany));
        assertThrows(IllegalArgumentException.class, () -> content(duplicateKeys));
    }

    @Test
    void allows_an_empty_draft_but_requires_a_milestone_before_publication() {
        // Arrange
        AdmissionsCallContent emptyDraft = content(List.of());

        // Act + Assert
        assertThrows(IllegalArgumentException.class, emptyDraft::requirePublishable);
    }

    private static AdmissionsMilestone milestone(int index, LocalDate date) {
        return new AdmissionsMilestone("milestone-" + index, AdmissionsMilestoneKind.APPLICATION,
                date, date, "Milestone " + index, "Description " + index);
    }

    private static AdmissionsCallContent content(List<AdmissionsMilestone> milestones) {
        AdmissionsSource source = new AdmissionsSource("Source", "https://example.edu/calendar");
        return new AdmissionsCallContent("Call title", "Call name", LocalDate.parse("2026-09-30"),
                LocalDate.parse("2026-10-01"), source, source, milestones);
    }
}
