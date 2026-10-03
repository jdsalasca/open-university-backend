package co.edu.uptc.universiry.notices.application;

import co.edu.uptc.universiry.notices.domain.InstitutionalNotice;
import co.edu.uptc.universiry.notices.domain.NoticeAudience;

import java.time.LocalDate;
import java.util.List;

public interface InstitutionalNoticeRepository {

    void append(InstitutionalNotice notice);

    List<InstitutionalNotice> recent(int limit);

    /**
     * Notices published on {@code date} whose audience is the university itself or one of {@code audiences}.
     * An empty audience still returns the university-wide notices.
     */
    List<InstitutionalNotice> effectiveOn(LocalDate date, List<NoticeAudience> audiences, int limit);
}