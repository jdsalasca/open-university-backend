package co.edu.uptc.universiry.library.domain;

import java.time.LocalDate;

import static co.edu.uptc.universiry.library.domain.LibraryText.required;

/** One physical exemplar. A copy is what a reader borrows, and only one loan of it can be open at a time. */
public record LibraryCopy(
        String copyId,
        String titleId,
        String barcode,
        String location,
        boolean active,
        String sourceReference
) {

    public LibraryCopy {
        copyId = required(copyId, 36, "copyId");
        titleId = required(titleId, 36, "titleId");
        barcode = required(barcode, 48, "barcode");
        location = required(location, 120, "location");
        sourceReference = required(sourceReference, 240, "sourceReference");
    }

    /**
     * A loan belongs to the canonical university user, never to a separate account table, and its due date is the one
     * the lending unit applied. The platform does not compute a loan period, because that rule is institutional.
     */
    public record Loan(
            String loanId,
            String copyId,
            String borrowerUserId,
            LocalDate lentOn,
            LocalDate dueOn,
            LocalDate returnedOn,
            String sourceReference
    ) {
        public Loan {
            loanId = required(loanId, 36, "loanId");
            copyId = required(copyId, 36, "copyId");
            borrowerUserId = required(borrowerUserId, 36, "borrowerUserId");
            sourceReference = required(sourceReference, 240, "sourceReference");
            if (lentOn == null || dueOn == null || dueOn.isBefore(lentOn)) {
                throw new IllegalArgumentException("a loan requires a due date that is not before the lending date");
            }
            if (returnedOn != null && returnedOn.isBefore(lentOn)) {
                throw new IllegalArgumentException("a returned loan cannot be closed before it was lent");
            }
        }

        public boolean open() {
            return returnedOn == null;
        }

        public Loan close(LocalDate returnedOn, String closingReference) {
            if (!open()) {
                throw new IllegalStateException("the loan was already returned");
            }
            return new Loan(loanId, copyId, borrowerUserId, lentOn, dueOn, returnedOn, closingReference);
        }
    }
}