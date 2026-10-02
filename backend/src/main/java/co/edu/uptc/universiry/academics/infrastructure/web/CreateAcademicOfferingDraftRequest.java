package co.edu.uptc.universiry.academics.infrastructure.web;

import co.edu.uptc.universiry.academics.application.AcademicOfferingDraftCommand;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;
import java.util.UUID;

public record CreateAcademicOfferingDraftRequest(
        @NotNull UUID periodId,
        @NotNull UUID curriculumId,
        @NotNull UUID subjectId,
        @NotBlank @Size(max = 24) String sectionCode,
        @NotNull LocalDate startsOn,
        @NotNull LocalDate endsOn,
        @Positive int proposedCapacity,
        @NotBlank @Size(max = 240) String sourceReference
) {
    AcademicOfferingDraftCommand toCommand() {
        return new AcademicOfferingDraftCommand(periodId, curriculumId, subjectId, sectionCode,
                startsOn, endsOn, proposedCapacity, sourceReference);
    }
}
