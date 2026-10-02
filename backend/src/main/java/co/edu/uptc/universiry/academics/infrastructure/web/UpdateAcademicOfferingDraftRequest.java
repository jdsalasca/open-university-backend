package co.edu.uptc.universiry.academics.infrastructure.web;

import co.edu.uptc.universiry.academics.application.AcademicOfferingDraftUpdateCommand;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

public record UpdateAcademicOfferingDraftRequest(
        @Positive int expectedVersion,
        @NotBlank @Size(max = 24) String sectionCode,
        @NotNull LocalDate startsOn,
        @NotNull LocalDate endsOn,
        @Positive int proposedCapacity,
        @NotBlank @Size(max = 240) String sourceReference
) {
    AcademicOfferingDraftUpdateCommand toCommand() {
        return new AcademicOfferingDraftUpdateCommand(expectedVersion, sectionCode, startsOn,
                endsOn, proposedCapacity, sourceReference);
    }
}
