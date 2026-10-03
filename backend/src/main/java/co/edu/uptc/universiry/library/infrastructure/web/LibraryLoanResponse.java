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
        String sourceReference
) {

    public static LibraryLoanResponse from(LibraryCopy.Loan loan) {
        return new LibraryLoanResponse(loan.loanId(), loan.copyId(), loan.borrowerUserId(),
                loan.lentOn(), loan.dueOn(), loan.returnedOn(), loan.sourceReference());
    }
}