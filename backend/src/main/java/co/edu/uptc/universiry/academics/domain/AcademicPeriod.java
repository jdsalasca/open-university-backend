package co.edu.uptc.universiry.academics.domain;

import java.time.Instant;
import java.time.LocalDate;
import java.util.Locale;
import java.util.UUID;
import java.util.regex.Pattern;

public record AcademicPeriod(
        UUID id,
        String code,
        AcademicPeriodKind kind,
        int academicYear,
        int sequenceNumber,
        LocalDate startsOn,
        LocalDate endsOn,
        AcademicPeriodStatus status,
        UUID approvedCalendarRevisionId,
        String approvalReference,
        String approvedBy,
        Instant approvedAt,
        String openedBy,
        Instant openedAt,
        String closedBy,
        Instant closedAt,
        String cancelledBy,
        Instant cancelledAt
) {
    private static final Pattern CODE = Pattern.compile("^[A-Z0-9][A-Z0-9._-]{0,63}$");

    public AcademicPeriod {
        if (id == null) throw new IllegalArgumentException("period.id is required");
        code = code == null ? "" : code.trim().toUpperCase(Locale.ROOT);
        if (!CODE.matcher(code).matches()) throw new IllegalArgumentException("period.code is invalid");
        if (kind == null) throw new IllegalArgumentException("period.kind is required");
        if (academicYear < 1900 || academicYear > 9999) throw new IllegalArgumentException("period.year is invalid");
        if (sequenceNumber < 1 || (kind == AcademicPeriodKind.REGULAR && sequenceNumber > 2)) {
            throw new IllegalArgumentException("period.sequenceNumber is invalid");
        }
        if (startsOn == null || endsOn == null || endsOn.isBefore(startsOn)) {
            throw new IllegalArgumentException("period.range is invalid");
        }
        if (status == null) throw new IllegalArgumentException("period.status is required");
        approvalReference = optionalReference(approvalReference);
        validateStateMetadata();
    }

    public static AcademicPeriod create(
            UUID id, String code, AcademicPeriodKind kind, int academicYear,
            int sequenceNumber, LocalDate startsOn, LocalDate endsOn
    ) {
        return new AcademicPeriod(id, code, kind, academicYear, sequenceNumber, startsOn, endsOn,
                AcademicPeriodStatus.DRAFT, null, null, null, null, null, null, null, null, null, null);
    }

    public AcademicPeriod approve(
            AcademicCalendarRevision calendar, String approvalReference, String actor, Instant at
    ) {
        requireStatus(AcademicPeriodStatus.DRAFT);
        if (calendar == null || calendar.status() != AcademicCalendarStatus.PUBLISHED
                || !calendar.periodId().equals(id) || calendar.officialReference() == null) {
            throw new IllegalArgumentException("period requires its published calendar and official reference");
        }
        String approvalAct = requiredReference(approvalReference);
        String approver = requiredActor(actor);
        if (at == null) throw new IllegalArgumentException("period.approvedAt is required");
        return new AcademicPeriod(id, code, kind, academicYear, sequenceNumber, startsOn, endsOn,
                AcademicPeriodStatus.APPROVED, calendar.id(), approvalAct, approver, at,
                null, null, null, null, null, null);
    }

    public AcademicPeriod open(String actor, Instant at) {
        requireStatus(AcademicPeriodStatus.APPROVED);
        if (approvedCalendarRevisionId == null) throw new IllegalArgumentException("period.calendar is required");
        String opener = requiredActor(actor);
        if (at == null) throw new IllegalArgumentException("period.openedAt is required");
        return new AcademicPeriod(id, code, kind, academicYear, sequenceNumber, startsOn, endsOn,
                AcademicPeriodStatus.OPEN, approvedCalendarRevisionId, approvalReference, approvedBy, approvedAt,
                opener, at, null, null, null, null);
    }

    public AcademicPeriod close(String actor, Instant at) {
        requireStatus(AcademicPeriodStatus.OPEN);
        String closer = requiredActor(actor);
        if (at == null) throw new IllegalArgumentException("period.closedAt is required");
        return new AcademicPeriod(id, code, kind, academicYear, sequenceNumber, startsOn, endsOn,
                AcademicPeriodStatus.CLOSED, approvedCalendarRevisionId, approvalReference, approvedBy, approvedAt,
                openedBy, openedAt, closer, at, null, null);
    }

    public AcademicPeriod cancel(String actor, Instant at) {
        if (status != AcademicPeriodStatus.DRAFT && status != AcademicPeriodStatus.APPROVED) {
            throw new AcademicPeriodStateConflictException();
        }
        String canceller = requiredActor(actor);
        if (at == null) throw new IllegalArgumentException("period.cancelledAt is required");
        return new AcademicPeriod(id, code, kind, academicYear, sequenceNumber, startsOn, endsOn,
                AcademicPeriodStatus.CANCELLED, approvedCalendarRevisionId, approvalReference, approvedBy, approvedAt,
                null, null, null, null, canceller, at);
    }

    public AcademicPeriod selectCalendarRevision(
            AcademicCalendarRevision activeCalendar, AcademicCalendarRevision calendar
    ) {
        if (status != AcademicPeriodStatus.APPROVED && status != AcademicPeriodStatus.OPEN) {
            throw new AcademicPeriodStateConflictException();
        }
        if (activeCalendar == null || activeCalendar.status() != AcademicCalendarStatus.PUBLISHED
                || !activeCalendar.id().equals(approvedCalendarRevisionId)
                || calendar == null || calendar.status() != AcademicCalendarStatus.PUBLISHED
                || !calendar.periodId().equals(id) || calendar.officialReference() == null) {
            throw new IllegalArgumentException("period requires its own published calendar and official reference");
        }
        if (calendar.version() <= activeCalendar.version()) {
            throw new AcademicPeriodStateConflictException();
        }
        return new AcademicPeriod(id, code, kind, academicYear, sequenceNumber, startsOn, endsOn,
                status, calendar.id(), approvalReference, approvedBy, approvedAt, openedBy, openedAt, closedBy, closedAt,
                cancelledBy, cancelledAt);
    }

    private void requireStatus(AcademicPeriodStatus expected) {
        if (status != expected) throw new AcademicPeriodStateConflictException();
    }

    private void validateStateMetadata() {
        if (status == AcademicPeriodStatus.DRAFT
                && (approvedCalendarRevisionId != null || approvalReference != null || approvedBy != null || approvedAt != null
                    || openedBy != null || openedAt != null || closedBy != null || closedAt != null
                    || cancelledBy != null || cancelledAt != null)) {
            throw new IllegalArgumentException("draft period cannot have transition metadata");
        }
        if (status == AcademicPeriodStatus.APPROVED
                && (approvedCalendarRevisionId == null || approvalReference == null || approvedBy == null || approvedAt == null)) {
            throw new IllegalArgumentException("approved period requires calendar and actor metadata");
        }
        if (status == AcademicPeriodStatus.OPEN && (approvedCalendarRevisionId == null || approvalReference == null
                || approvedBy == null || approvedAt == null
                || openedBy == null || openedAt == null)) {
            throw new IllegalArgumentException("open period requires approval and opening metadata");
        }
        if (status == AcademicPeriodStatus.CLOSED && (approvedCalendarRevisionId == null || approvalReference == null
                || approvedBy == null || approvedAt == null
                || openedBy == null || openedAt == null || closedBy == null || closedAt == null)) {
            throw new IllegalArgumentException("closed period requires approval, opening, and closing metadata");
        }
        if (status == AcademicPeriodStatus.CANCELLED) {
            boolean hasApproval = approvedBy != null || approvedAt != null || approvedCalendarRevisionId != null
                    || approvalReference != null;
            if (cancelledBy == null || cancelledAt == null
                    || (hasApproval && (approvedCalendarRevisionId == null || approvalReference == null
                    || approvedBy == null || approvedAt == null))) {
                throw new IllegalArgumentException("cancelled period metadata is invalid");
            }
        }
    }

    private static String optionalReference(String value) {
        if (value == null || value.isBlank()) return null;
        String reference = value.trim();
        if (reference.length() > 240) throw new IllegalArgumentException("period.approvalReference is too long");
        return reference;
    }

    private static String requiredReference(String value) {
        String reference = optionalReference(value);
        if (reference == null) throw new IllegalArgumentException("period.approvalReference is required");
        return reference;
    }

    private static String requiredActor(String value) {
        if (value == null || value.isBlank() || value.trim().length() > 180) {
            throw new IllegalArgumentException("period.actor is invalid");
        }
        return value.trim();
    }
}
