package co.edu.uptc.universiry.academics.domain;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class AcademicSubjectRevisionTest {

    @Test
    void rejects_nonpositive_excessive_or_overprecision_credits() {
        // Arrange
        UUID revisionId = UUID.randomUUID();
        UUID subjectId = UUID.randomUUID();
        BigDecimal[] invalidCredits = {
                BigDecimal.ZERO,
                new BigDecimal("-0.25"),
                new BigDecimal("1000"),
                new BigDecimal("1.001")
        };

        // Act + Assert
        for (BigDecimal credits : invalidCredits) {
            assertThrows(IllegalArgumentException.class,
                    () -> revision(revisionId, subjectId, "Fundamentos", credits));
        }
    }

    @Test
    void rejects_missing_identity_or_overlong_subject_name() {
        // Arrange
        UUID revisionId = UUID.randomUUID();
        UUID subjectId = UUID.randomUUID();
        String tooLongName = "A".repeat(241);

        // Act + Assert
        assertThrows(IllegalArgumentException.class,
                () -> revision(null, subjectId, "Fundamentos", new BigDecimal("3")));
        assertThrows(IllegalArgumentException.class,
                () -> revision(revisionId, null, "Fundamentos", new BigDecimal("3")));
        assertThrows(IllegalArgumentException.class,
                () -> revision(revisionId, subjectId, " ", new BigDecimal("3")));
        assertThrows(IllegalArgumentException.class,
                () -> revision(revisionId, subjectId, tooLongName, new BigDecimal("3")));
    }

    @Test
    void trims_names_and_canonicalizes_valid_credit_precision() {
        // Arrange
        UUID revisionId = UUID.randomUUID();
        UUID subjectId = UUID.randomUUID();

        // Act
        AcademicSubjectRevision revision = revision(
                revisionId, subjectId, " Álgebra lineal ", new BigDecimal("3.00"));

        // Assert
        assertEquals("Álgebra lineal", revision.subjectName());
        assertEquals(new BigDecimal("3"), revision.credits());
    }

    private AcademicSubjectRevision revision(UUID id, UUID subjectId, String name, BigDecimal credits) {
        return new AcademicSubjectRevision(id, subjectId, name, credits);
    }
}
