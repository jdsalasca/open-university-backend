package co.edu.uptc.universiry.academics.domain;

import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AcademicStructureRulesTest {

    @Test
    void treats_validity_endpoints_as_inclusive_and_open_ended_ranges_as_unbounded() {
        // Arrange
        LocalDate boundary = LocalDate.of(2026, 12, 31);

        // Act + Assert
        assertTrue(AcademicStructureRules.overlaps(LocalDate.of(2026, 1, 1), boundary,
                boundary, null));
        assertFalse(AcademicStructureRules.overlaps(LocalDate.of(2026, 1, 1), boundary,
                LocalDate.of(2027, 1, 1), null));
        assertTrue(AcademicStructureRules.containedBy(LocalDate.of(2026, 3, 1), boundary,
                LocalDate.of(2026, 1, 1), null));
        assertFalse(AcademicStructureRules.containedBy(LocalDate.of(2026, 3, 1), null,
                LocalDate.of(2026, 1, 1), boundary));
    }

    @Test
    void rejects_indirect_cycles_only_when_their_effective_dates_overlap() {
        // Arrange
        UUID faculty = UUID.randomUUID();
        UUID school = UUID.randomUUID();
        UUID programUnit = UUID.randomUUID();
        List<AcademicOrganizationRelation> existing = List.of(
                edge(faculty, school, "2026-01-01", "2026-12-31"),
                edge(school, programUnit, "2026-05-01", null));
        AcademicOrganizationRelation simultaneous = edge(programUnit, faculty, "2026-06-01", null);
        AcademicOrganizationRelation later = edge(programUnit, faculty, "2027-01-01", null);

        // Act + Assert
        assertTrue(AcademicStructureRules.createsCycle(simultaneous, existing));
        assertFalse(AcademicStructureRules.createsCycle(later, existing));
        assertThrows(IllegalArgumentException.class,
                () -> new AcademicOrganizationRelation(faculty, school,
                        0, LocalDate.of(2026, 1, 1), LocalDate.of(2025, 12, 31)));
    }

    private static AcademicOrganizationRelation edge(UUID parent, UUID child, String from, String through) {
        return new AcademicOrganizationRelation(parent, child, 0, LocalDate.parse(from),
                through == null ? null : LocalDate.parse(through));
    }
}
