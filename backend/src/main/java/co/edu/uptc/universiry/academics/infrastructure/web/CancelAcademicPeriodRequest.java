package co.edu.uptc.universiry.academics.infrastructure.web;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CancelAcademicPeriodRequest(@NotBlank @Size(max = 240) String reference) {
}
