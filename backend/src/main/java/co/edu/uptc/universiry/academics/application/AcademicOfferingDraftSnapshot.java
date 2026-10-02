package co.edu.uptc.universiry.academics.application;

import co.edu.uptc.universiry.academics.domain.AcademicOfferingDraft;

import java.time.LocalDate;

public record AcademicOfferingDraftSnapshot(
        String sectionCode,
        LocalDate startsOn,
        LocalDate endsOn,
        int proposedCapacity,
        int version
) {
    public static AcademicOfferingDraftSnapshot from(AcademicOfferingDraft draft) {
        return new AcademicOfferingDraftSnapshot(draft.sectionCode(), draft.startsOn(), draft.endsOn(),
                draft.proposedCapacity(), draft.version());
    }
}
