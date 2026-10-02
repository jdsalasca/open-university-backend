package co.edu.uptc.universiry.academics.infrastructure.web;

import co.edu.uptc.universiry.academics.domain.AcademicOfferingDraft;

import java.util.UUID;

public record AcademicOfferingDraftMutationResponse(UUID id, int version, String status) {
    static AcademicOfferingDraftMutationResponse from(AcademicOfferingDraft draft) {
        return new AcademicOfferingDraftMutationResponse(draft.id(), draft.version(), "DRAFT");
    }
}
