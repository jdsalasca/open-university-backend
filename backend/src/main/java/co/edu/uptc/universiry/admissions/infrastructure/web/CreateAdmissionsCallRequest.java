package co.edu.uptc.universiry.admissions.infrastructure.web;

import co.edu.uptc.universiry.admissions.domain.AdmissionsCallContent;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record CreateAdmissionsCallRequest(
        @NotBlank @Size(max = 64) @Pattern(regexp = "[a-z0-9]+(?:-[a-z0-9]+)*") String callKey,
        @NotNull @Valid AdmissionsCallContent content
) {
}
