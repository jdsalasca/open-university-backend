package co.edu.uptc.universiry.academics.domain;

import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class AcademicProgramTest {

    @Test
    void rejects_invalid_program_and_campus_codes() {
        // Arrange
        UUID id = UUID.randomUUID();
        String tooLongCode = "A".repeat(65);

        // Act + Assert
        assertThrows(IllegalArgumentException.class, () -> program(id, null, "TUNJA"));
        assertThrows(IllegalArgumentException.class, () -> program(id, "  ", "TUNJA"));
        assertThrows(IllegalArgumentException.class, () -> program(id, "bad code", "TUNJA"));
        assertThrows(IllegalArgumentException.class, () -> program(id, tooLongCode, "TUNJA"));
        assertThrows(IllegalArgumentException.class, () -> program(id, "ING-01", ""));
        assertThrows(IllegalArgumentException.class, () -> program(id, "ING-01", "C".repeat(65)));
        assertThrows(IllegalArgumentException.class, () -> program(id, "ING-01", "campus name"));
        assertThrows(IllegalArgumentException.class,
                () -> new AcademicSubject(null, "MAT-001"));
        assertThrows(IllegalArgumentException.class,
                () -> new AcademicSubject(UUID.randomUUID(), " "));
        assertThrows(IllegalArgumentException.class,
                () -> new AcademicSubject(UUID.randomUUID(), "M".repeat(65)));
    }

    @Test
    void normalizes_stable_codes_and_keeps_the_in_person_undergraduate_scope() {
        // Arrange
        UUID id = UUID.randomUUID();

        // Act
        AcademicProgram program = program(id, " ing-01 ", " tunja ");

        // Assert
        assertEquals("ING-01", program.programCode());
        assertEquals(AcademicLevel.PREGRADO, program.academicLevel());
        assertEquals(StudyModality.PRESENCIAL, program.studyModality());
        assertEquals("TUNJA", program.campusCode());
    }

    @Test
    void requires_program_identity_and_revision_labels() {
        // Arrange
        UUID id = UUID.randomUUID();
        UUID programId = UUID.randomUUID();

        // Act + Assert
        assertThrows(IllegalArgumentException.class, () -> new AcademicProgram(
                null, "ING-01", AcademicLevel.PREGRADO, StudyModality.PRESENCIAL, "TUNJA"));
        assertThrows(IllegalArgumentException.class, () -> new AcademicProgram(
                id, "ING-01", null, StudyModality.PRESENCIAL, "TUNJA"));
        assertThrows(IllegalArgumentException.class, () -> new AcademicProgram(
                id, "ING-01", AcademicLevel.PREGRADO, null, "TUNJA"));
        assertThrows(IllegalArgumentException.class, () -> new AcademicProgramRevision(
                UUID.randomUUID(), programId, null, " ", "Ingeniería", "Tunja"));
        assertThrows(IllegalArgumentException.class, () -> new AcademicProgramRevision(
                UUID.randomUUID(), programId, "S".repeat(33), "Ingeniería", "Facultad", "Tunja"));
        assertThrows(IllegalArgumentException.class, () -> new AcademicProgramRevision(
                UUID.randomUUID(), programId, null, "P".repeat(241), "Facultad", "Tunja"));
    }

    private AcademicProgram program(UUID id, String programCode, String campusCode) {
        return new AcademicProgram(
                id,
                programCode,
                AcademicLevel.PREGRADO,
                StudyModality.PRESENCIAL,
                campusCode
        );
    }
}
