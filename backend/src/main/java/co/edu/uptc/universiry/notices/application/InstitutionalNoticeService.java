package co.edu.uptc.universiry.notices.application;

import co.edu.uptc.universiry.notices.domain.InstitutionalNotice;
import co.edu.uptc.universiry.notices.domain.NoticeAudience;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;

public interface InstitutionalNoticeService {

    InstitutionalNotice publish(PublishInstitutionalNoticeCommand command, String actorSub);

    InstitutionalNoticePage recent(int limit);

    /** Notices visible on {@code date} to a caller holding {@code audiences}. */
    InstitutionalNoticePage visibleOn(LocalDate date, Collection<NoticeAudience> audiences, int limit);

    record InstitutionalNoticePage(List<InstitutionalNotice> notices) {
        public InstitutionalNoticePage {
            notices = notices == null ? List.of() : List.copyOf(notices);
        }
    }
}