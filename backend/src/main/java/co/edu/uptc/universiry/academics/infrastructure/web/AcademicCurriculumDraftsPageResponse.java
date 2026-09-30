package co.edu.uptc.universiry.academics.infrastructure.web;

import co.edu.uptc.universiry.academics.application.AcademicCurriculumDraftsPage;

import java.util.List;

public record AcademicCurriculumDraftsPageResponse(
        int pageSize,
        long totalItems,
        List<AcademicCurriculumResponse> drafts,
        String nextCursor
) {

    public AcademicCurriculumDraftsPageResponse {
        drafts = List.copyOf(drafts);
    }

    public static AcademicCurriculumDraftsPageResponse from(AcademicCurriculumDraftsPage page) {
        return new AcademicCurriculumDraftsPageResponse(
                page.pageSize(), page.totalItems(),
                page.drafts().stream().map(AcademicCurriculumResponse::from).toList(),
                page.nextCursor() == null ? null : page.nextCursor().encode());
    }
}
