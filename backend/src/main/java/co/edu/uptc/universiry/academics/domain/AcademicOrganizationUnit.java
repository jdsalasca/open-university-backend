package co.edu.uptc.universiry.academics.domain;

import java.time.LocalDate;
import java.util.Locale;
import java.util.UUID;
import java.util.regex.Pattern;

public record AcademicOrganizationUnit(
        UUID id,
        String code,
        AcademicOrganizationUnitType type,
        String displayName,
        int displayOrder,
        AcademicEntityStatus status,
        LocalDate validFrom,
        LocalDate validThrough
) {
    private static final Pattern CODE = Pattern.compile("^[A-Z0-9][A-Z0-9._-]{0,63}$");

    public AcademicOrganizationUnit {
        if (id == null) throw new IllegalArgumentException("organizationUnit.id is required");
        if (type == null) throw new IllegalArgumentException("organizationUnit.type is required");
        if (status == null) throw new IllegalArgumentException("organizationUnit.status is required");
        code = code == null ? "" : code.trim().toUpperCase(Locale.ROOT);
        if (!CODE.matcher(code).matches()) {
            throw new IllegalArgumentException("organizationUnit.code is invalid");
        }
        displayName = displayName == null ? "" : displayName.trim();
        if (displayName.isEmpty() || displayName.length() > 240) {
            throw new IllegalArgumentException("organizationUnit.displayName is invalid");
        }
        if (displayOrder < 0) throw new IllegalArgumentException("organizationUnit.displayOrder is invalid");
        if (validFrom == null) throw new IllegalArgumentException("organizationUnit.validFrom is required");
        if (validThrough != null && validThrough.isBefore(validFrom)) {
            throw new IllegalArgumentException("organizationUnit.validThrough is invalid");
        }
    }

    public AcademicOrganizationUnit(
            UUID id,
            String code,
            AcademicOrganizationUnitType type,
            String displayName,
            int displayOrder,
            LocalDate validFrom,
            LocalDate validThrough
    ) {
        this(id, code, type, displayName, displayOrder, AcademicEntityStatus.ACTIVE, validFrom, validThrough);
    }
}
