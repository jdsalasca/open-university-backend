package co.edu.uptc.universiry.notices.infrastructure.web;

import co.edu.uptc.universiry.notices.domain.NoticeAudienceKind;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;
import java.util.List;

public record PublishInstitutionalNoticeRequest(
        @NotBlank @Size(max = 160) String title,
        @NotBlank @Size(max = 2000) String body,
        @NotBlank @Size(max = 240) String sourceReference,
        @NotNull LocalDate publishedFrom,
        @NotNull LocalDate publishedThrough,
        @NotEmpty @Size(max = 50) List<@Valid NoticeAudienceRequest> audiences
) {

    public record NoticeAudienceRequest(@NotNull NoticeAudienceKind kind, @Size(max = 64) String reference) {
    }
}