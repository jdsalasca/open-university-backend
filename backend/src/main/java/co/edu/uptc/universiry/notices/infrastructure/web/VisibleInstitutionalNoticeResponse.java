package co.edu.uptc.universiry.notices.infrastructure.web;

import co.edu.uptc.universiry.notices.application.InstitutionalNoticeService;
import co.edu.uptc.universiry.notices.domain.InstitutionalNotice;
import co.edu.uptc.universiry.notices.domain.NoticeAudienceKind;

import java.time.LocalDate;
import java.util.List;

/**
 * The audience-facing projection. It deliberately omits the publishing actor and the institutional reference, because
 * a reader receives the content of a notice, not the internal trail of who issued it.
 */
public record VisibleInstitutionalNoticeResponse(
        String noticeId,
        String title,
        String body,
        LocalDate publishedFrom,
        LocalDate publishedThrough,
        List<VisibleAudienceResponse> audiences
) {

    public record VisibleAudienceResponse(NoticeAudienceKind kind, String reference) {
    }

    public static VisibleInstitutionalNoticeResponse from(InstitutionalNotice notice) {
        return new VisibleInstitutionalNoticeResponse(
                notice.noticeId(),
                notice.title(),
                notice.body(),
                notice.publishedFrom(),
                notice.publishedThrough(),
                notice.audiences().stream()
                        .map(audience -> new VisibleAudienceResponse(audience.kind(), audience.reference()))
                        .toList());
    }

    public record VisibleNoticesResponse(List<VisibleInstitutionalNoticeResponse> notices) {
        public VisibleNoticesResponse {
            notices = notices == null ? List.of() : List.copyOf(notices);
        }
    }
}