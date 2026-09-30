package co.edu.uptc.universiry.academics.domain;

import java.time.LocalDate;
import java.util.Locale;
import java.util.UUID;
import java.util.regex.Pattern;

public record AcademicSite(
        UUID id,
        String code,
        AcademicSiteType type,
        String displayName,
        int displayOrder,
        AcademicEntityStatus status,
        LocalDate validFrom,
        LocalDate validThrough
) {
    private static final Pattern CODE = Pattern.compile("^[A-Z0-9][A-Z0-9._-]{0,63}$");

    public AcademicSite {
        if (id == null) throw new IllegalArgumentException("site.id is required");
        if (type == null) throw new IllegalArgumentException("site.type is required");
        if (status == null) throw new IllegalArgumentException("site.status is required");
        code = code == null ? "" : code.trim().toUpperCase(Locale.ROOT);
        if (!CODE.matcher(code).matches()) throw new IllegalArgumentException("site.code is invalid");
        displayName = displayName == null ? "" : displayName.trim();
        if (displayName.isEmpty() || displayName.length() > 240) {
            throw new IllegalArgumentException("site.displayName is invalid");
        }
        if (displayOrder < 0) throw new IllegalArgumentException("site.displayOrder is invalid");
        if (validFrom == null || validThrough != null && validThrough.isBefore(validFrom)) {
            throw new IllegalArgumentException("site.validity is invalid");
        }
    }

    public static AcademicSite active(
            UUID id, String code, AcademicSiteType type, String displayName, int displayOrder,
            LocalDate validFrom, LocalDate validThrough
    ) {
        return new AcademicSite(id, code, type, displayName, displayOrder,
                AcademicEntityStatus.ACTIVE, validFrom, validThrough);
    }
}
