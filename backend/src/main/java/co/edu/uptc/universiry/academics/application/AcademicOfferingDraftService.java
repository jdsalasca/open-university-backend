package co.edu.uptc.universiry.academics.application;

import co.edu.uptc.universiry.academics.domain.AcademicOfferingDraft;

import java.util.UUID;

public interface AcademicOfferingDraftService {
    AcademicOfferingDraftPage drafts(UUID periodId, int limit, String before);

    AcademicOfferingAuditPage history(UUID offeringId, int limit, String before);

    AcademicOfferingDraft create(AcademicOfferingDraftCommand command, String actorSub);

    AcademicOfferingDraft update(UUID offeringId, AcademicOfferingDraftUpdateCommand command, String actorSub);
}
