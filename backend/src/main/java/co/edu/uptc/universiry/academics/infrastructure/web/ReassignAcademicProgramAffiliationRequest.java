package co.edu.uptc.universiry.academics.infrastructure.web;

import co.edu.uptc.universiry.academics.application.AcademicProgramAffiliationReassignmentCommand;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;
import java.util.UUID;

public record ReassignAcademicProgramAffiliationRequest(
        @NotNull LocalDate expectedValidFrom,
        @JsonProperty(value = "expectedValidThrough", required = true) LocalDate expectedValidThrough,
        @NotNull LocalDate effectiveFrom,
        @NotNull UUID organizationUnitId,
        @NotNull UUID siteId,
        @NotNull @Min(0) @Max(100_000) Integer displayOrder,
        @NotBlank @Size(max = 240) String sourceReference
) {
    AcademicProgramAffiliationReassignmentCommand toCommand() {
        return new AcademicProgramAffiliationReassignmentCommand(expectedValidFrom, expectedValidThrough,
                effectiveFrom, organizationUnitId, siteId, displayOrder, sourceReference);
    }
}
