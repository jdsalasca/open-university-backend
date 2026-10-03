package co.edu.uptc.universiry.library.domain;

import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LibraryDomainTest {

    private static final LocalDate LENT_ON = LocalDate.parse("2026-10-03");
    private static final LocalDate DUE_ON = LocalDate.parse("2026-10-17");

    @Test
    void a_bibliographic_record_trims_its_authors_and_rejects_an_empty_credit() {
        // Arrange + Act
        var title = new LibraryTitle("t-1", "  Álgebra lineal  ", List.of("  autor uno ", "autor dos"), "3a", 2019, "Acta 1");

        // Assert
        assertEquals("Álgebra lineal", title.title());
        assertEquals(List.of("autor uno", "autor dos"), title.authors());
        assertThrows(IllegalArgumentException.class,
                () -> new LibraryTitle("t-2", "Sin autores", List.of(), "1a", null, "Acta 1"));
        assertThrows(IllegalArgumentException.class,
                () -> new LibraryTitle("t-3", "Demasiados", java.util.stream.IntStream.range(0, 21)
                        .mapToObj(index -> "autor " + index).toList(), "1a", null, "Acta 1"));
    }

    @Test
    void a_bibliographic_record_rejects_a_plausible_but_wrong_publication_year() {
        // Arrange + Act + Assert
        assertThrows(IllegalArgumentException.class,
                () -> new LibraryTitle("t-1", "Título", List.of("autor"), "1a", 1200, "Acta 1"));
        assertThrows(IllegalArgumentException.class,
                () -> new LibraryTitle("t-1", "Título", List.of("autor"), "1a", 2400, "Acta 1"));
    }

    @Test
    void a_loan_requires_a_due_date_that_is_not_before_the_lending_date() {
        // Arrange + Act + Assert
        assertThrows(IllegalArgumentException.class, () -> loan(LENT_ON.minusDays(1)));
        assertEquals(DUE_ON, loan(DUE_ON).dueOn());
        // A loan that is due the same day it was lent is valid: the platform does not invent a loan period.
        assertEquals(LENT_ON, loan(LENT_ON).dueOn());
    }

    @Test
    void a_loan_is_open_until_it_is_returned_and_cannot_be_returned_twice() {
        // Arrange
        var open = loan(DUE_ON);

        // Act
        var returned = open.close(LENT_ON.plusDays(5), "Acta de devolución");

        // Assert
        assertTrue(open.open());
        assertFalse(returned.open());
        assertEquals(LENT_ON.plusDays(5), returned.returnedOn());
        assertThrows(IllegalStateException.class, () -> returned.close(LENT_ON.plusDays(6), "Acta 2"));
    }

    @Test
    void a_loan_cannot_be_returned_before_it_was_lent() {
        // Arrange + Act + Assert
        assertThrows(IllegalArgumentException.class, () -> loan(DUE_ON).close(LENT_ON.minusDays(1), "Acta"));
    }

    @Test
    void a_copy_requires_a_barcode_and_a_location() {
        // Arrange + Act + Assert
        assertThrows(IllegalArgumentException.class, () -> new LibraryCopy("c-1", "t-1", " ", "Estante 1", true, "Acta 1"));
        assertThrows(IllegalArgumentException.class, () -> new LibraryCopy("c-1", "t-1", "BC-1", "", true, "Acta 1"));
    }

    @Test
    void a_loan_points_to_the_canonical_university_user() {
        // Arrange + Act
        var open = loan(DUE_ON);

        // Assert
        assertEquals("4b1f9c2a-5d3e-4a76-9b8c-2f0e7d1a4c55", open.borrowerUserId());
        assertThrows(IllegalArgumentException.class,
                () -> new LibraryCopy.Loan("l-1", "c-1", " ", LENT_ON, DUE_ON, null, "Acta 1"));
    }

    private static LibraryCopy.Loan loan(LocalDate dueOn) {
        return new LibraryCopy.Loan("l-1", "c-1", "4b1f9c2a-5d3e-4a76-9b8c-2f0e7d1a4c55",
                LENT_ON, dueOn, null, "Acta de préstamo 1");
    }

    @Test
    void a_copy_keeps_its_withdrawal_trail_in_step_with_its_circulation_state() {
        // Arrange: a circulating copy has no trail; a withdrawn one must carry the whole trail.
        java.time.Instant leftCirculation = java.time.Instant.EPOCH;

        // Act + Assert
        assertThrows(IllegalArgumentException.class, () -> new LibraryCopy(
                "c-1", "t-1", "BC-1", "Estante 1", true, "Acta 1", "actor", "Resolución 1", null));
        assertThrows(IllegalArgumentException.class, () -> new LibraryCopy(
                "c-1", "t-1", "BC-1", "Estante 1", false, "Acta 1", null, null, null));
        assertEquals("actor", new LibraryCopy(
                "c-1", "t-1", "BC-1", "Estante 1", false, "Acta 1", "actor", "Resolución 1", leftCirculation)
                .withdrawnBy());
    }
}