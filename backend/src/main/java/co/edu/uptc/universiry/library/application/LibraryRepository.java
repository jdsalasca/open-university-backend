package co.edu.uptc.universiry.library.application;

import co.edu.uptc.universiry.library.domain.LibraryCopy;
import co.edu.uptc.universiry.library.domain.LibraryTitle;

import java.util.List;
import java.util.Optional;

public interface LibraryRepository {

    void appendTitle(LibraryTitle title, String actorSub);

    void appendCopy(LibraryCopy copy, String actorSub);

    List<LibraryTitle> titles(int limit);

    List<LibraryCopy> copiesOf(String titleId);

    /** Locks the copy row so that a second open loan cannot be created for the same exemplar. */
    Optional<LibraryCopy> lockCopy(String copyId);

    Optional<LibraryCopy.Loan> openLoanOf(String copyId);

    /** Finds a loan whatever its state, so closing an already returned loan is reported as a conflict, not as a miss. */
    Optional<LibraryCopy.Loan> findLoan(String loanId);

    /** Opens the loan recording the acting subject, so lending is auditable and not only returning is. */
    void appendLoan(LibraryCopy.Loan loan, String actorSub);

    /** Removes a circulating copy from circulation recording who acted and under which institutional reference. */
    void markCopyWithdrawn(String copyId, String actorSub, String reference, java.time.Instant at);

    /** Closes the loan recording the acting subject separately from the institutional reference. */
    void closeLoan(LibraryCopy.Loan loan, String actorSub);

    List<LibraryCopy.Loan> loansOf(String borrowerUserId, int limit);
}