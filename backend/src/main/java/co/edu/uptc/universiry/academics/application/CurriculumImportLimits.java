package co.edu.uptc.universiry.academics.application;

import co.edu.uptc.universiry.academics.domain.AcademicCatalogLimits;

public record CurriculumImportLimits(int maxFileBytes, int maxRows) {

    public CurriculumImportLimits {
        if (maxFileBytes < 1 || maxFileBytes > AcademicCatalogLimits.MAX_IMPORT_BYTES) {
            throw new IllegalArgumentException("maxFileBytes is outside the supported range.");
        }
        if (maxRows < 1 || maxRows > AcademicCatalogLimits.MAX_IMPORT_ROWS) {
            throw new IllegalArgumentException("maxRows is outside the supported range.");
        }
    }
}
