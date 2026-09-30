package co.edu.uptc.universiry.academics.domain;

import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class AcademicStructureRelationTest {

    @Test
    void organization_relation_accepts_zero_and_rejects_negative_display_order() {
        // Arrange
        UUID parent = UUID.randomUUID();
        UUID child = UUID.randomUUID();
        LocalDate validFrom = LocalDate.of(2026, 1, 1);

        // Act
        AcademicOrganizationRelation relation = new AcademicOrganizationRelation(
                parent, child, 0, validFrom, null);

        // Assert
        assertEquals(0, relation.displayOrder());
        assertThrows(IllegalArgumentException.class, () -> new AcademicOrganizationRelation(
                parent, child, -1, validFrom, null));
    }

    @Test
    void site_relation_accepts_zero_and_rejects_negative_display_order() {
        // Arrange
        UUID parent = UUID.randomUUID();
        UUID child = UUID.randomUUID();
        LocalDate validFrom = LocalDate.of(2026, 1, 1);

        // Act
        AcademicSiteRelation relation = new AcademicSiteRelation(parent, child, 0, validFrom, null);

        // Assert
        assertEquals(0, relation.displayOrder());
        assertThrows(IllegalArgumentException.class, () -> new AcademicSiteRelation(
                parent, child, -1, validFrom, null));
    }
}
