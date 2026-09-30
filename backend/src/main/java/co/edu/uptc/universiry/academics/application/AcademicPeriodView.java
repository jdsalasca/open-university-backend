package co.edu.uptc.universiry.academics.application;

import co.edu.uptc.universiry.academics.domain.AcademicPeriod;

import java.time.Instant;
import java.util.UUID;

public record AcademicPeriodView(
        AcademicPeriod period,
        Integer calendarRevisionNumber,
        String officialReference,
        Instant createdAt
) {
    public UUID id() { return period.id(); }
}
