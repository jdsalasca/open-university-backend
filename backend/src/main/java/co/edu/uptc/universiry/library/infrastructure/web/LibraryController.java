package co.edu.uptc.universiry.library.infrastructure.web;

import co.edu.uptc.universiry.library.application.LibraryService;
import co.edu.uptc.universiry.library.application.LibraryService.LendCommand;
import co.edu.uptc.universiry.library.application.LibraryService.RegisterCopyCommand;
import co.edu.uptc.universiry.library.application.LibraryService.RegisterTitleCommand;
import co.edu.uptc.universiry.library.infrastructure.web.RegisterLibraryTitleRequest.LendLibraryCopyRequest;
import co.edu.uptc.universiry.library.infrastructure.web.RegisterLibraryTitleRequest.RegisterLibraryCopyRequest;
import co.edu.uptc.universiry.library.infrastructure.web.RegisterLibraryTitleRequest.ReturnLibraryCopyRequest;
import co.edu.uptc.universiry.library.infrastructure.web.RegisterLibraryTitleRequest.WithdrawLibraryCopyRequest;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

import static org.springframework.http.HttpStatus.CREATED;

@RestController
public class LibraryController {

    private final LibraryService library;

    public LibraryController(LibraryService library) {
        this.library = library;
    }

    @GetMapping("/api/v1/admin/library/titles")
    public List<LibraryTitleResponse> titles(@RequestParam(defaultValue = "25") int limit) {
        return library.titles(limit).stream().map(LibraryTitleResponse::from).toList();
    }

    @GetMapping("/api/v1/admin/library/titles/{titleId}/copies")
    public List<LibraryCopyResponse> copies(@PathVariable String titleId,
                                            @RequestParam(defaultValue = "50") int limit) {
        return library.copiesOf(titleId, limit).stream().map(LibraryCopyResponse::from).toList();
    }

    @GetMapping("/api/v1/admin/library/loans")
    public List<LibraryLoanResponse> loansOf(@RequestParam String borrowerUserId,
                                            @RequestParam(defaultValue = "25") int limit) {
        return library.loansOf(borrowerUserId, limit).stream().map(LibraryLoanResponse::from).toList();
    }

    @PostMapping("/api/v1/admin/library/titles")
    public ResponseEntity<LibraryTitleResponse> registerTitle(
            @Valid @RequestBody RegisterLibraryTitleRequest request, Authentication authentication) {
        var created = library.registerTitle(new RegisterTitleCommand(
                request.title(), request.authors(), request.edition(),
                request.publicationYear(), request.sourceReference()), authentication.getName());
        return ResponseEntity.status(CREATED).body(LibraryTitleResponse.from(created));
    }

    @PostMapping("/api/v1/admin/library/titles/{titleId}/copies")
    public ResponseEntity<LibraryCopyResponse> registerCopy(
            @PathVariable String titleId,
            @Valid @RequestBody RegisterLibraryCopyRequest request,
            Authentication authentication) {
        var created = library.registerCopy(new RegisterCopyCommand(
                titleId, request.barcode(), request.location(), request.sourceReference()), authentication.getName());
        return ResponseEntity.status(CREATED).body(LibraryCopyResponse.from(created));
    }

    @PostMapping("/api/v1/admin/library/loans")
    public ResponseEntity<LibraryLoanResponse> lend(
            @Valid @RequestBody LendLibraryCopyRequest request, Authentication authentication) {
        var created = library.lend(new LendCommand(
                request.copyId(), request.borrowerUserId(), request.lentOn(), request.dueOn(),
                request.sourceReference()), authentication.getName());
        return ResponseEntity.status(CREATED).body(LibraryLoanResponse.from(created));
    }

    @PostMapping("/api/v1/admin/library/loans/{loanId}/return")
    public LibraryLoanResponse returnCopy(
            @PathVariable String loanId,
            @Valid @RequestBody ReturnLibraryCopyRequest request,
            Authentication authentication) {
        return LibraryLoanResponse.from(library.returnCopy(
                loanId, request.returnedOn(), request.sourceReference(), authentication.getName()));
    }

    @PostMapping("/api/v1/admin/library/copies/{copyId}/withdraw")
    public LibraryCopyResponse withdrawCopy(
            @PathVariable String copyId,
            @Valid @RequestBody WithdrawLibraryCopyRequest request,
            Authentication authentication) {
        return LibraryCopyResponse.from(
                library.withdrawCopy(copyId, request.sourceReference(), authentication.getName()));
    }
}