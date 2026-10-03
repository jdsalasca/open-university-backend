package co.edu.uptc.universiry.library.application;

import co.edu.uptc.universiry.identity.application.IdentityDirectory;
import co.edu.uptc.universiry.library.domain.LibraryCopy;
import co.edu.uptc.universiry.library.domain.LibraryTitle;

import java.time.Clock;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Catalogue and lending. The due date is always supplied by the lending unit and the platform never derives a loan
 * period, because that rule belongs to the institution. Every administrative action records its institutional
 * reference and its actor.
 */
@Service
public class DefaultLibraryService implements LibraryService {

    private static final int MAX_PAGE_SIZE = 100;

    private final LibraryRepository repository;
    private final IdentityDirectory identities;
    private final Clock clock;

    public DefaultLibraryService(LibraryRepository repository, IdentityDirectory identities, Clock clock) {
        this.repository = repository;
        this.identities = identities;
        this.clock = clock;
    }

    @Override
    @Transactional
    public LibraryTitle registerTitle(LibraryService.RegisterTitleCommand command, String actorSub) {
        requireActor(actorSub);
        LibraryTitle title = new LibraryTitle(newIdentifier(), command.title(), command.authors(), command.edition(),
                command.publicationYear(), command.sourceReference());
        repository.appendTitle(title, actorSub);
        return title;
    }

    @Override
    @Transactional
    public LibraryCopy registerCopy(LibraryService.RegisterCopyCommand command, String actorSub) {
        requireActor(actorSub);
        LibraryCopy copy = new LibraryCopy(newIdentifier(), command.titleId(), command.barcode(),
                command.location(), true, command.sourceReference());
        repository.appendCopy(copy, actorSub);
        return copy;
    }

    @Override
    @Transactional
    public LibraryCopy.Loan lend(LibraryService.LendCommand command, String actorSub) {
        requireActor(actorSub);
        // The borrower is always the canonical university user. A library never keeps its own account table.
        if (!identities.userExists(parseUserId(command.borrowerUserId()))) {
            throw new IllegalArgumentException("the borrower is not a registered university user");
        }
        LibraryCopy copy = repository.lockCopy(command.copyId())
                .orElseThrow(() -> new LibraryCopyNotFoundException(command.copyId()));
        if (!copy.active()) {
            throw new LibraryLoanRejectedException("the copy is withdrawn from circulation");
        }
        repository.openLoanOf(copy.copyId()).ifPresent(open -> {
            throw new LibraryLoanRejectedException("the copy already has an open loan");
        });
        LibraryCopy.Loan loan = new LibraryCopy.Loan(newIdentifier(), copy.copyId(), command.borrowerUserId(),
                command.lentOn(), command.dueOn(), null, command.sourceReference());
        repository.appendLoan(loan, actorSub);
        return loan;
    }

    @Override
    @Transactional
    public LibraryCopy.Loan returnCopy(String loanId, LocalDate returnedOn, String sourceReference, String actorSub) {
        requireActor(actorSub);
        LibraryCopy.Loan loan = repository.findLoan(loanId)
                .orElseThrow(() -> new LibraryLoanNotFoundException(loanId));
        LibraryCopy.Loan closed = loan.close(
                returnedOn == null ? LocalDate.now(clock) : returnedOn, sourceReference);
        repository.closeLoan(closed, actorSub);
        return closed;
    }

    @Override
    @Transactional
    public LibraryCopy withdrawCopy(String copyId, String sourceReference, String actorSub) {
        requireActor(actorSub);
        requireReference(sourceReference);
        // Locking the copy row serializes withdrawal against lending, so a loan cannot be opened on a copy that is
        // leaving circulation in the same instant.
        LibraryCopy copy = repository.lockCopy(copyId)
                .orElseThrow(() -> new LibraryCopyNotFoundException(copyId));
        if (!copy.active()) {
            throw new IllegalStateException("the copy is already withdrawn from circulation");
        }
        repository.markCopyWithdrawn(copyId, actorSub, sourceReference, clock.instant());
        // Re-read the row so the response carries the persisted withdrawal trail instead of a local guess.
        return repository.lockCopy(copyId)
                .orElseThrow(() -> new LibraryCopyNotFoundException(copyId));
    }

    @Override
    @Transactional(readOnly = true)
    public List<LibraryTitle> titles(String query, int limit) {
        return repository.titles(query, pageSize(limit));
    }

    @Override
    @Transactional(readOnly = true)
    public List<LibraryCopy> copiesOf(String titleId, int limit) {
        return repository.copiesOf(titleId).stream().limit(pageSize(limit)).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<LibraryCopy.Loan> loansOf(String borrowerUserId, int limit) {
        return repository.loansOf(borrowerUserId, pageSize(limit));
    }

    @Override
    @Transactional(readOnly = true)
    public List<LibraryCopy.Loan> openLoans(int limit) {
        return repository.openLoans(pageSize(limit));
    }

    private static void requireActor(String actorSub) {
        if (actorSub == null || actorSub.isBlank()) {
            throw new IllegalArgumentException("a library action requires an authenticated actor");
        }
    }

    private static void requireReference(String reference) {
        if (reference == null || reference.isBlank()) {
            throw new IllegalArgumentException("a library action requires an institutional reference");
        }
    }

    private static String newIdentifier() {
        return UUID.randomUUID().toString();
    }

    private static UUID parseUserId(String borrowerUserId) {
        try {
            return UUID.fromString(borrowerUserId.trim());
        } catch (IllegalArgumentException invalidIdentifier) {
            throw new IllegalArgumentException("the borrower is not a canonical university user identifier");
        }
    }

    private static int pageSize(int limit) {
        if (limit < 1 || limit > MAX_PAGE_SIZE) {
            throw new IllegalArgumentException("a library page size must be between 1 and " + MAX_PAGE_SIZE);
        }
        return limit;
    }
}