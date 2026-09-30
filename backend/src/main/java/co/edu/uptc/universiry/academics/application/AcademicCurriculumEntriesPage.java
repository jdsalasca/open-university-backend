package co.edu.uptc.universiry.academics.application;

import java.util.List;
import java.util.UUID;

public record AcademicCurriculumEntriesPage(
        UUID curriculumId,
        int page,
        int pageSize,
        long totalItems,
        int totalPages,
        List<AcademicCurriculumEntrySummary> entries
) {

    public AcademicCurriculumEntriesPage {
        entries = List.copyOf(entries);
    }
}
