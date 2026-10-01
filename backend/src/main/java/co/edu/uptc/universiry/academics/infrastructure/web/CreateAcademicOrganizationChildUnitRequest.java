package co.edu.uptc.universiry.academics.infrastructure.web;

import co.edu.uptc.universiry.academics.application.AcademicOrganizationChildUnitCommand;
import co.edu.uptc.universiry.academics.domain.AcademicOrganizationUnitType;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

public record CreateAcademicOrganizationChildUnitRequest(
        @NotBlank @Size(max = 64) String code,
        @NotNull AcademicOrganizationUnitType type,
        @NotBlank @Size(max = 240) String displayName,
        @Min(0) int displayOrder,
        @NotNull LocalDate validFrom,
        LocalDate validThrough,
        @NotBlank @Size(max = 240) String sourceReference
) {
    AcademicOrganizationChildUnitCommand toCommand() {
        return new AcademicOrganizationChildUnitCommand(code, type, displayName, displayOrder,
                validFrom, validThrough, sourceReference);
    }
}
