package co.edu.uptc.universiry.academics.infrastructure.web;

import co.edu.uptc.universiry.academics.application.AcademicOrganizationRelationCloseCommand;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

public record CloseAcademicOrganizationRelationRequest(
        @NotNull LocalDate validFrom,
        @NotNull LocalDate effectiveThrough,
        @NotBlank @Size(max = 240) String sourceReference
) {
    AcademicOrganizationRelationCloseCommand toCommand() {
        return new AcademicOrganizationRelationCloseCommand(validFrom, effectiveThrough, sourceReference);
    }
}
