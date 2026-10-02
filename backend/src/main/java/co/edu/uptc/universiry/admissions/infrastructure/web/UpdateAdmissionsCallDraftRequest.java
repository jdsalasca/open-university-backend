package co.edu.uptc.universiry.admissions.infrastructure.web;

import co.edu.uptc.universiry.admissions.domain.AdmissionsCallContent;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record UpdateAdmissionsCallDraftRequest(
        @Positive int expectedDraftVersion,
        @NotNull @Valid AdmissionsCallContent content
) {
}
