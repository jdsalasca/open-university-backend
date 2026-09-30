package co.edu.uptc.universiry.academics.infrastructure.web;

import co.edu.uptc.universiry.academics.application.AcademicPeriodCreateCommand;
import co.edu.uptc.universiry.academics.domain.AcademicPeriodKind;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;

public record CreateAcademicPeriodRequest(
        @NotBlank String code,
        @NotNull AcademicPeriodKind kind,
        @Min(1900) @Max(9999) int academicYear,
        @Min(1) @Max(99) int sequenceNumber,
        @NotNull LocalDate startsOn,
        @NotNull LocalDate endsOn
) {
    AcademicPeriodCreateCommand toCommand() {
        return new AcademicPeriodCreateCommand(code, kind, academicYear, sequenceNumber, startsOn, endsOn);
    }
}
