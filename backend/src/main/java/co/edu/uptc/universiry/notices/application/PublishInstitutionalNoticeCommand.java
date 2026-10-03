package co.edu.uptc.universiry.notices.application;

import co.edu.uptc.universiry.notices.domain.NoticeAudience;

import java.time.LocalDate;
import java.util.List;

/** The administrative command to publish one notice for one audience. */
public record PublishInstitutionalNoticeCommand(
        String title,
        String body,
        String sourceReference,
        LocalDate publishedFrom,
        LocalDate publishedThrough,
        List<NoticeAudience> audiences
) {
    public PublishInstitutionalNoticeCommand {
        if (title == null || body == null || sourceReference == null
                || publishedFrom == null || publishedThrough == null || audiences == null) {
            throw new IllegalArgumentException("a notice command requires every field");
        }
    }
}