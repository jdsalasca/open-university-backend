package co.edu.uptc.universiry.academics.infrastructure.web;

import co.edu.uptc.universiry.academics.application.AcademicOfferingDraftPage;
import co.edu.uptc.universiry.academics.application.AcademicOfferingDraftView;
import co.edu.uptc.universiry.academics.domain.AcademicPeriodKind;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public record AcademicOfferingDraftPageResponse(List<OfferingDraftResponse> drafts, String nextCursor) {
    public AcademicOfferingDraftPageResponse {
        drafts = List.copyOf(drafts);
    }

    static AcademicOfferingDraftPageResponse from(AcademicOfferingDraftPage page) {
        return new AcademicOfferingDraftPageResponse(page.drafts().stream().map(OfferingDraftResponse::from).toList(),
                page.nextCursor());
    }

    public record OfferingDraftResponse(
            UUID id,
            UUID periodId,
            String periodCode,
            AcademicPeriodKind periodKind,
            UUID curriculumId,
            String curriculumVersion,
            String programCode,
            String programName,
            UUID subjectId,
            String subjectCode,
            String subjectName,
            String sectionCode,
            LocalDate startsOn,
            LocalDate endsOn,
            int proposedCapacity,
            int version,
            String status,
            String sourceReference,
            String updatedBy,
            Instant createdAt,
            Instant updatedAt
    ) {
        static OfferingDraftResponse from(AcademicOfferingDraftView view) {
            var draft = view.draft();
            return new OfferingDraftResponse(draft.id(), draft.periodId(), view.periodCode(), view.periodKind(),
                    draft.curriculumId(), view.curriculumVersion(), view.programCode(), view.programName(),
                    draft.subjectId(), view.subjectCode(), view.subjectName(), draft.sectionCode(),
                    draft.startsOn(), draft.endsOn(), draft.proposedCapacity(), draft.version(), "DRAFT",
                    view.sourceReference(), view.updatedBy(), view.createdAt(), view.updatedAt());
        }
    }
}
