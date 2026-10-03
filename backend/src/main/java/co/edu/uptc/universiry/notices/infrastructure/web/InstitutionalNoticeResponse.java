package co.edu.uptc.universiry.notices.infrastructure.web;

import co.edu.uptc.universiry.notices.application.InstitutionalNoticeService;
import co.edu.uptc.universiry.notices.domain.InstitutionalNotice;
import co.edu.uptc.universiry.notices.domain.NoticeAudience;
import co.edu.uptc.universiry.notices.domain.NoticeAudienceKind;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

public record InstitutionalNoticeResponse(
        String noticeId,
        String title,
        String body,
        String sourceReference,
        LocalDate publishedFrom,
        LocalDate publishedThrough,
        List<NoticeAudienceResponse> audiences,
        String publishedBy,
        Instant publishedAt
) {

    public record NoticeAudienceResponse(NoticeAudienceKind kind, String reference) {
    }

    public static InstitutionalNoticeResponse from(InstitutionalNotice notice) {
        return new InstitutionalNoticeResponse(
                notice.noticeId(),
                notice.title(),
                notice.body(),
                notice.sourceReference(),
                notice.publishedFrom(),
                notice.publishedThrough(),
                notice.audiences().stream()
                        .map(audience -> new NoticeAudienceResponse(audience.kind(), audience.reference()))
                        .toList(),
                notice.publishedBy(),
                notice.publishedAt());
    }
}