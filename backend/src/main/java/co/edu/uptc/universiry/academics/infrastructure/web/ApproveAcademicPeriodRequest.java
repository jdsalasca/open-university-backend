package co.edu.uptc.universiry.academics.infrastructure.web;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.util.UUID;

public record ApproveAcademicPeriodRequest(
        @NotNull UUID calendarRevisionId,
        @NotBlank @Size(max = 240) String approvalReference
) {
}
