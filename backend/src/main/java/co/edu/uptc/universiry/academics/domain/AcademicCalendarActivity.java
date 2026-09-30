package co.edu.uptc.universiry.academics.domain;

import java.time.LocalDateTime;
import java.util.Locale;
import java.util.UUID;
import java.util.regex.Pattern;

public record AcademicCalendarActivity(
        String key,
        String label,
        LocalDateTime startsAt,
        LocalDateTime endsAt,
        UUID organizationUnitId,
        UUID siteId
) {
    private static final Pattern KEY = Pattern.compile("^[A-Z][A-Z0-9_]{0,63}$");

    public AcademicCalendarActivity {
        key = key == null ? "" : key.trim().toUpperCase(Locale.ROOT);
        if (!KEY.matcher(key).matches()) throw new IllegalArgumentException("calendarActivity.key is invalid");
        label = label == null ? "" : label.trim();
        if (label.isEmpty() || label.length() > 160) {
            throw new IllegalArgumentException("calendarActivity.label is invalid");
        }
        if (startsAt == null || endsAt == null || endsAt.isBefore(startsAt)) {
            throw new IllegalArgumentException("calendarActivity.range is invalid");
        }
    }

    public AcademicCalendarActivity(String key, String label, LocalDateTime startsAt, LocalDateTime endsAt) {
        this(key, label, startsAt, endsAt, null, null);
    }
}
