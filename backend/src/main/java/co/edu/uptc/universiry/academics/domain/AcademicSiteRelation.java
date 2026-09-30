package co.edu.uptc.universiry.academics.domain;

import java.time.LocalDate;
import java.util.UUID;

public record AcademicSiteRelation(
        UUID parentSiteId,
        UUID childSiteId,
        LocalDate validFrom,
        LocalDate validThrough
) {
    public AcademicSiteRelation {
        if (parentSiteId == null || childSiteId == null || parentSiteId.equals(childSiteId)) {
            throw new IllegalArgumentException("siteRelation.endpoints are invalid");
        }
        AcademicStructureRules.validateInterval(validFrom, validThrough, "siteRelation");
    }
}
