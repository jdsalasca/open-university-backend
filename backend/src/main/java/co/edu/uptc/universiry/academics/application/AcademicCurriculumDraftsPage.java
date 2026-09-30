package co.edu.uptc.universiry.academics.application;

import java.util.List;

public record AcademicCurriculumDraftsPage(
        int pageSize,
        long totalItems,
        List<CurriculumSummary> drafts,
        CurriculumDraftCursor nextCursor
) {

    public AcademicCurriculumDraftsPage {
        drafts = List.copyOf(drafts);
    }
}
