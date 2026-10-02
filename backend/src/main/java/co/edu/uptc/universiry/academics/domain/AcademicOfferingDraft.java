package co.edu.uptc.universiry.academics.domain;

import java.time.LocalDate;
import java.util.Locale;
import java.util.UUID;
import java.util.regex.Pattern;

public record AcademicOfferingDraft(
        UUID id,
        UUID periodId,
        UUID curriculumId,
        UUID subjectId,
        String sectionCode,
        LocalDate startsOn,
        LocalDate endsOn,
        int proposedCapacity,
        int version
) {
    private static final Pattern SECTION_CODE = Pattern.compile("^[A-Z0-9][A-Z0-9._-]{0,23}$");

    public AcademicOfferingDraft {
        if (id == null) throw new IllegalArgumentException("offering.id is required");
        if (periodId == null) throw new IllegalArgumentException("offering.periodId is required");
        if (curriculumId == null) throw new IllegalArgumentException("offering.curriculumId is required");
        if (subjectId == null) throw new IllegalArgumentException("offering.subjectId is required");
        sectionCode = normalizeSectionCode(sectionCode);
        if (startsOn == null || endsOn == null || endsOn.isBefore(startsOn)) {
            throw new IllegalArgumentException("offering.dateRange is invalid");
        }
        if (proposedCapacity < 1) throw new IllegalArgumentException("offering.proposedCapacity is invalid");
        if (version < 1) throw new IllegalArgumentException("offering.version is invalid");
    }

    public static AcademicOfferingDraft create(
            UUID id,
            UUID periodId,
            UUID curriculumId,
            UUID subjectId,
            String sectionCode,
            LocalDate startsOn,
            LocalDate endsOn,
            int proposedCapacity
    ) {
        return new AcademicOfferingDraft(id, periodId, curriculumId, subjectId, sectionCode,
                startsOn, endsOn, proposedCapacity, 1);
    }

    public AcademicOfferingDraft revise(
            int expectedVersion,
            String sectionCode,
            LocalDate startsOn,
            LocalDate endsOn,
            int proposedCapacity
    ) {
        if (expectedVersion != version) throw new AcademicOfferingVersionConflictException();
        return new AcademicOfferingDraft(id, periodId, curriculumId, subjectId, sectionCode,
                startsOn, endsOn, proposedCapacity, version + 1);
    }

    private static String normalizeSectionCode(String value) {
        String normalized = value == null ? "" : value.trim().toUpperCase(Locale.ROOT);
        if (!SECTION_CODE.matcher(normalized).matches()) {
            throw new IllegalArgumentException("offering.sectionCode is invalid");
        }
        return normalized;
    }
}
