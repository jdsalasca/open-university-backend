package co.edu.uptc.universiry.academics.domain;

import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class AcademicOrganizationUnitTest {

    @Test
    void normalizesStableCodeAndDisplayName() {
        // Arrange
        AcademicOrganizationUnit source = new AcademicOrganizationUnit(
                UUID.randomUUID(), "  fac-01 ", AcademicOrganizationUnitType.FACULTY,
                "  Facultad de Ciencias  ", 4, LocalDate.of(2026, 1, 1), null);

        // Act + Assert
        assertEquals("FAC-01", source.code());
        assertEquals("Facultad de Ciencias", source.displayName());
    }

    @Test
    void rejectsUnsupportedStableCode() {
        // Arrange + Act + Assert
        assertThrows(IllegalArgumentException.class, () -> new AcademicOrganizationUnit(
                UUID.randomUUID(), "faculty/01", AcademicOrganizationUnitType.FACULTY,
                "Facultad de Ciencias", 1, LocalDate.of(2026, 1, 1), null));
    }

    @Test
    void rejectsNegativeDisplayOrder() {
        // Arrange + Act + Assert
        assertThrows(IllegalArgumentException.class, () -> new AcademicOrganizationUnit(
                UUID.randomUUID(), "FAC-01", AcademicOrganizationUnitType.FACULTY,
                "Facultad de Ciencias", -1, LocalDate.of(2026, 1, 1), null));
    }

    @Test
    void rejectsValidityEndingBeforeItStarts() {
        // Arrange + Act + Assert
        assertThrows(IllegalArgumentException.class, () -> new AcademicOrganizationUnit(
                UUID.randomUUID(), "FAC-01", AcademicOrganizationUnitType.FACULTY,
                "Facultad de Ciencias", 1, LocalDate.of(2026, 2, 1), LocalDate.of(2026, 1, 31)));
    }

    @Test
    void rejectsMissingIdentityOrType() {
        // Arrange + Act + Assert
        assertThrows(IllegalArgumentException.class, () -> new AcademicOrganizationUnit(
                null, "FAC-01", AcademicOrganizationUnitType.FACULTY,
                "Facultad de Ciencias", 1, LocalDate.of(2026, 1, 1), null));
        assertThrows(IllegalArgumentException.class, () -> new AcademicOrganizationUnit(
                UUID.randomUUID(), "FAC-01", null,
                "Facultad de Ciencias", 1, LocalDate.of(2026, 1, 1), null));
    }
}
