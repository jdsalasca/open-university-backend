package co.edu.uptc.universiry.library.infrastructure.web;

import co.edu.uptc.universiry.library.domain.LibraryCopy;

import java.time.LocalDate;

public record LibraryLoanResponse(
        String loanId,
        String copyId,
        String borrowerUserId,
        LocalDate lentOn,
        LocalDate dueOn,
        LocalDate returnedOn,
        boolean overdue,
        String sourceReference
) {

    public static LibraryLoanResponse from(LibraryCopy.Loan loan) {
        return from(loan, null);
    }

    /** Uses the institutional date to tell whether an outstanding loan is already behind its due date. */
    public static LibraryLoanResponse from(LibraryCopy.Loan loan, LocalDate today) {
        boolean overdue = loan.open() && today != null && loan.dueOn().isBefore(today);
        return new LibraryLoanResponse(loan.loanId(), loan.copyId(), loan.borrowerUserId(),
                loan.lentOn(), loan.dueOn(), loan.returnedOn(), overdue, loan.sourceReference());
    }
}