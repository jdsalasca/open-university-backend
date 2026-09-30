package co.edu.uptc.universiry.academics.infrastructure.web;

import co.edu.uptc.universiry.academics.application.AcademicProgramAffiliationCommand;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;
import java.util.UUID;

public record CreateAcademicProgramAffiliationRequest(
        @NotNull UUID organizationUnitId,
        @NotNull UUID siteId,
        @NotNull @Min(0) @Max(100_000) Integer displayOrder,
        @NotNull LocalDate validFrom,
        LocalDate validThrough,
        @NotBlank @Size(max = 240) String sourceReference
) {
    AcademicProgramAffiliationCommand toCommand() {
        return new AcademicProgramAffiliationCommand(
                organizationUnitId, siteId, displayOrder, validFrom, validThrough, sourceReference);
    }
}
