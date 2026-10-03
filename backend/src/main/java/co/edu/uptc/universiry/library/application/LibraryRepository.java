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

    void appendLoan(LibraryCopy.Loan loan);

    void closeLoan(LibraryCopy.Loan loan);

    List<LibraryCopy.Loan> loansOf(String borrowerUserId, int limit);
}