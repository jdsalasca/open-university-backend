package co.edu.uptc.universiry.notices.application;

import co.edu.uptc.universiry.notices.domain.InstitutionalNotice;

import java.util.List;

public interface InstitutionalNoticeService {

    InstitutionalNotice publish(PublishInstitutionalNoticeCommand command, String actorSub);

    InstitutionalNoticePage recent(int limit);

    record InstitutionalNoticePage(List<InstitutionalNotice> notices) {
        public InstitutionalNoticePage {
            notices = notices == null ? List.of() : List.copyOf(notices);
        }
    }
}