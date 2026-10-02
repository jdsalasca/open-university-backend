package co.edu.uptc.universiry.admissions.infrastructure.web;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.util.UUID;

public record PublishAdmissionsCallRevisionRequest(
        @Positive int expectedDraftVersion,
        UUID expectedPublishedRevisionId,
        @NotBlank @Size(max = 240) String officialReference
) {
}
