package co.edu.uptc.universiry.academics.infrastructure.web;

import co.edu.uptc.universiry.academics.application.AcademicCurriculumEntriesPage;

import java.util.List;
import java.util.UUID;

public record AcademicCurriculumEntriesPageResponse(
        UUID curriculumId,
        int page,
        int pageSize,
        long totalItems,
        int totalPages,
        List<AcademicCurriculumEntryResponse> entries
) {

    public AcademicCurriculumEntriesPageResponse {
        entries = List.copyOf(entries);
    }

    public static AcademicCurriculumEntriesPageResponse from(AcademicCurriculumEntriesPage page) {
        return new AcademicCurriculumEntriesPageResponse(
                page.curriculumId(), page.page(), page.pageSize(), page.totalItems(), page.totalPages(),
                page.entries().stream().map(AcademicCurriculumEntryResponse::from).toList());
    }
}
