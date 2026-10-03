package co.edu.uptc.universiry.notices.application;

import co.edu.uptc.universiry.notices.domain.InstitutionalNotice;
import co.edu.uptc.universiry.notices.domain.NoticeAudience;

import java.time.Clock;
import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Publication is append only. The domain validates every invariant before anything is stored, and the notice plus
 * its audit event are written in one transaction so a notice can never exist without its publication record.
 */
@Service
public class DefaultInstitutionalNoticeService implements InstitutionalNoticeService {

    private static final int MAX_PAGE_SIZE = 100;

    private final InstitutionalNoticeRepository repository;
    private final Clock clock;

    public DefaultInstitutionalNoticeService(InstitutionalNoticeRepository repository, Clock clock) {
        this.repository = repository;
        this.clock = clock;
    }

    @Override
    @Transactional
    public InstitutionalNotice publish(PublishInstitutionalNoticeCommand command, String actorSub) {
        if (actorSub == null || actorSub.isBlank()) {
            throw new IllegalArgumentException("a notice requires an authenticated actor");
        }
        InstitutionalNotice notice = new InstitutionalNotice(
                UUID.randomUUID().toString(),
                command.title(),
                command.body(),
                command.sourceReference(),
                command.publishedFrom(),
                command.publishedThrough(),
                command.audiences(),
                actorSub,
                clock.instant());
        repository.append(notice);
        return notice;
    }

    @Override
    @Transactional(readOnly = true)
    public InstitutionalNoticePage recent(int limit) {
        return new InstitutionalNoticePage(repository.recent(pageSize(limit)));
    }

    @Override
    @Transactional(readOnly = true)
    public InstitutionalNoticePage visibleOn(LocalDate date, Collection<NoticeAudience> audiences, int limit) {
        if (date == null) {
            throw new IllegalArgumentException("a visibility date is required");
        }
        List<NoticeAudience> scopes = audiences == null ? List.of() : List.copyOf(audiences);
        return new InstitutionalNoticePage(repository.effectiveOn(date, scopes, pageSize(limit)));
    }

    private static int pageSize(int limit) {
        if (limit < 1 || limit > MAX_PAGE_SIZE) {
            throw new IllegalArgumentException("a notice page size must be between 1 and " + MAX_PAGE_SIZE);
        }
        return limit;
    }
}