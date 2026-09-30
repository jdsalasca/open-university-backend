package co.edu.uptc.universiry.branding.infrastructure.web;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record BrandingRollbackRequest(
        @NotNull @Positive Long targetRevision,
        @NotNull @Positive Long expectedRevision
) {
}
