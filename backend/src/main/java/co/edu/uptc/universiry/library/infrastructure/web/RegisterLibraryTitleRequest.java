package co.edu.uptc.universiry.library.infrastructure.web;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;
import java.util.List;

public record RegisterLibraryTitleRequest(
        @NotBlank @Size(max = 240) String title,
        @NotEmpty @Size(max = 20) List<@NotBlank @Size(max = 160) String> authors,
        @NotBlank @Size(max = 80) String edition,
        @Min(1450) @Max(2200) Integer publicationYear,
        @NotBlank @Size(max = 240) String sourceReference
) {

    public record RegisterLibraryCopyRequest(
            @NotBlank @Size(max = 48) String barcode,
            @NotBlank @Size(max = 120) String location,
            @NotBlank @Size(max = 240) String sourceReference
    ) {
    }

    public record LendLibraryCopyRequest(
            @NotBlank String copyId,
            @NotBlank String borrowerUserId,
            @NotNull LocalDate lentOn,
            @NotNull LocalDate dueOn,
            @NotBlank @Size(max = 240) String sourceReference
    ) {
    }

    public record ReturnLibraryCopyRequest(LocalDate returnedOn, @NotBlank @Size(max = 240) String sourceReference) {
    }
}