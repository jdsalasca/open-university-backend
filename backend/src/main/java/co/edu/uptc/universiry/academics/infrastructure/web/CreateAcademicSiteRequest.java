package co.edu.uptc.universiry.academics.infrastructure.web;

import co.edu.uptc.universiry.academics.application.AcademicSiteCommand;
import co.edu.uptc.universiry.academics.domain.AcademicSiteType;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

public record CreateAcademicSiteRequest(
        @NotBlank @Size(max = 64) String code,
        @NotNull AcademicSiteType type,
        @NotBlank @Size(max = 240) String displayName,
        @Min(0) int displayOrder,
        @NotNull LocalDate validFrom,
        LocalDate validThrough,
        @NotBlank @Size(max = 240) String sourceReference
) {
    AcademicSiteCommand toCommand() {
        return new AcademicSiteCommand(code, type, displayName, displayOrder, validFrom, validThrough, sourceReference);
    }
}
