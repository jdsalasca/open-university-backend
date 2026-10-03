package co.edu.uptc.universiry.library.infrastructure.persistence;

import co.edu.uptc.universiry.identity.application.IdentityDirectory;
import co.edu.uptc.universiry.identity.domain.AuthenticatedPrincipal;
import co.edu.uptc.universiry.library.application.LibraryRepository;
import co.edu.uptc.universiry.library.application.LibraryService;
import co.edu.uptc.universiry.library.application.LibraryService.LendCommand;
import co.edu.uptc.universiry.library.application.LibraryService.RegisterCopyCommand;
import co.edu.uptc.universiry.library.application.LibraryService.RegisterTitleCommand;
import co.edu.uptc.universiry.library.domain.LibraryCopy;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertThrows;

@SpringBootTest(properties = "spring.datasource.url=jdbc:h2:mem:library-loan-race;MODE=MySQL;DB_CLOSE_DELAY=-1;DATABASE_TO_LOWER=TRUE")
@ActiveProfiles("test")
@Transactional
class LibraryLoanConcurrencyTest {

    @Autowired
    private LibraryService library;

    @Autowired
    private LibraryRepository repository;

    @Autowired
    private IdentityDirectory identities;

    @Test
    void a_second_close_of_the_same_loan_is_rejected_instead_of_silently_reporting_success() {
        // Arrange: a copy with one open loan, plus the closed value the first caller would persist.
        String titleId = library.registerTitle(new RegisterTitleCommand(
                "Física universitaria", List.of("Autor"), "2a", 2020, "Acta 1 de 2026"), "librarian").titleId();
        String copyId = library.registerCopy(new RegisterCopyCommand(
                titleId, "BC-9001", "Estante B-1", "Acta 2 de 2026"), "librarian").copyId();
        String borrowerId = identities.registerIfAbsent(
                        new AuthenticatedPrincipal("https://identity.example.edu", "library.racer"), Instant.EPOCH)
                .userId().toString();
        LibraryCopy.Loan closed = library.lend(new LendCommand(
                        copyId, borrowerId, LocalDate.of(2026, 10, 3), LocalDate.of(2026, 10, 17), "Préstamo 1 de 2026"),
                "librarian").close(LocalDate.of(2026, 10, 9), "Devolución 1 de 2026");
        repository.closeLoan(closed, "librarian");

        // Act + Assert: the row is already closed, so a competing close must fail loudly, not report success.
        assertThrows(IllegalStateException.class, () -> repository.closeLoan(closed, "librarian"));
    }
}
