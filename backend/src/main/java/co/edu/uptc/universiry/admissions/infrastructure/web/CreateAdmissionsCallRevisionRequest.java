package co.edu.uptc.universiry.admissions.infrastructure.web;

import co.edu.uptc.universiry.admissions.domain.AdmissionsCallContent;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

public record CreateAdmissionsCallRevisionRequest(@NotNull @Valid AdmissionsCallContent content) {
}
