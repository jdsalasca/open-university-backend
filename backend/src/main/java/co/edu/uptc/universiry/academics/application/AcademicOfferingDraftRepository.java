package co.edu.uptc.universiry.academics.application;

import co.edu.uptc.universiry.academics.domain.AcademicOfferingDraft;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

public interface AcademicOfferingDraftRepository {
    AcademicOfferingDraftPage findByPeriod(UUID periodId, int limit, AcademicOfferingDraftCursor before);

    Optional<AcademicOfferingDraft> find(UUID id);

    void create(AcademicOfferingDraft draft, String reference, String actor, Instant at);

    void update(AcademicOfferingDraft draft, int expectedVersion, String reference, String actor, Instant at);

    AcademicOfferingAuditPage findAuditEvents(UUID offeringId, int limit, Long beforeId);
}
