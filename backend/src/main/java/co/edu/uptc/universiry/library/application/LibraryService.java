package co.edu.uptc.universiry.library.application;

import co.edu.uptc.universiry.library.domain.LibraryCopy;
import co.edu.uptc.universiry.library.domain.LibraryTitle;

import java.time.LocalDate;
import java.util.List;

public interface LibraryService {

    LibraryTitle registerTitle(RegisterTitleCommand command, String actorSub);

    LibraryCopy registerCopy(RegisterCopyCommand command, String actorSub);

    LibraryCopy.Loan lend(LendCommand command, String actorSub);

    LibraryCopy.Loan returnCopy(String loanId, LocalDate returnedOn, String sourceReference, String actorSub);

    LibraryCopy withdrawCopy(String copyId, String sourceReference, String actorSub);

    LibraryCopy copyOfBarcode(String barcode);

    List<LibraryTitle> titles(String query, int limit);

    List<LibraryCopy> copiesOf(String titleId, int limit);

    List<LibraryCopy.Loan> loansOf(String borrowerUserId, int limit);

    List<LibraryCopy.Loan> openLoans(int limit);

    record RegisterTitleCommand(
            String title,
            List<String> authors,
            String edition,
            Integer publicationYear,
            String sourceReference
    ) {
    }

    record RegisterCopyCommand(String titleId, String barcode, String location, String sourceReference) {
    }

    record LendCommand(
            String copyId,
            String borrowerUserId,
            LocalDate lentOn,
            LocalDate dueOn,
            String sourceReference
    ) {
    }
}