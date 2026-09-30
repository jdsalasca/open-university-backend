package co.edu.uptc.universiry.academics.domain;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public final class AcademicCalendarRevision {
    private final UUID id;
    private final UUID periodId;
    private final int version;
    private final String officialReference;
    private final AcademicCalendarStatus status;
    private final List<AcademicCalendarActivity> activities;
    private final String publishedBy;
    private final Instant publishedAt;

    private AcademicCalendarRevision(
            UUID id,
            UUID periodId,
            int version,
            String officialReference,
            AcademicCalendarStatus status,
            List<AcademicCalendarActivity> activities,
            String publishedBy,
            Instant publishedAt
    ) {
        if (id == null || periodId == null) throw new IllegalArgumentException("calendar.id is required");
        if (version < 1) throw new IllegalArgumentException("calendar.version is invalid");
        if (status == null) throw new IllegalArgumentException("calendar.status is required");
        this.id = id;
        this.periodId = periodId;
        this.version = version;
        this.officialReference = optionalText(officialReference, 240);
        this.status = status;
        this.activities = activities == null ? List.of() : List.copyOf(activities);
        this.publishedBy = optionalText(publishedBy, 180);
        this.publishedAt = publishedAt;
        if (status == AcademicCalendarStatus.PUBLISHED
                && (this.officialReference == null || this.publishedBy == null || publishedAt == null || this.activities.isEmpty())) {
            throw new IllegalArgumentException("published calendar requires a reference, actor, time, and activities");
        }
        if (status != AcademicCalendarStatus.PUBLISHED && (this.publishedBy != null || publishedAt != null)) {
            throw new IllegalArgumentException("unpublished calendar cannot have publication metadata");
        }
    }

    public static AcademicCalendarRevision draft(
            UUID id, UUID periodId, int version, String officialReference,
            List<AcademicCalendarActivity> activities
    ) {
        return new AcademicCalendarRevision(
                id, periodId, version, officialReference, AcademicCalendarStatus.DRAFT, activities, null, null);
    }

    public static AcademicCalendarRevision restore(
            UUID id, UUID periodId, int version, String officialReference, AcademicCalendarStatus status,
            List<AcademicCalendarActivity> activities, String publishedBy, Instant publishedAt
    ) {
        return new AcademicCalendarRevision(
                id, periodId, version, officialReference, status, activities, publishedBy, publishedAt);
    }

    public AcademicCalendarRevision publish(AcademicPeriod period, String actor, Instant at) {
        if (status != AcademicCalendarStatus.DRAFT) {
            throw new AcademicPeriodStateConflictException();
        }
        if (period == null || !periodId.equals(period.id())) {
            throw new IllegalArgumentException("calendar.periodId does not match");
        }
        if (officialReference == null || activities.isEmpty()) {
            throw new IllegalArgumentException("calendar requires an official reference and activities");
        }
        return new AcademicCalendarRevision(
                id, periodId, version, officialReference, AcademicCalendarStatus.PUBLISHED,
                activities, requiredActor(actor), requiredInstant(at));
    }

    public UUID id() { return id; }
    public UUID periodId() { return periodId; }
    public int version() { return version; }
    public String officialReference() { return officialReference; }
    public AcademicCalendarStatus status() { return status; }
    public List<AcademicCalendarActivity> activities() { return activities; }
    public String publishedBy() { return publishedBy; }
    public Instant publishedAt() { return publishedAt; }

    private static String optionalText(String value, int maximumLength) {
        if (value == null || value.isBlank()) return null;
        String trimmed = value.trim();
        if (trimmed.length() > maximumLength) throw new IllegalArgumentException("calendar text is too long");
        return trimmed;
    }

    private static String requiredActor(String value) {
        String actor = optionalText(value, 180);
        if (actor == null) throw new IllegalArgumentException("calendar.actor is required");
        return actor;
    }

    private static Instant requiredInstant(Instant value) {
        if (value == null) throw new IllegalArgumentException("calendar.publishedAt is required");
        return value;
    }
}
