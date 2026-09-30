package co.edu.uptc.universiry.academics.application;

import java.util.List;
import java.util.UUID;

public interface AcademicCatalogQueryService {

    List<AcademicProgramSummary> publishedPrograms();

    List<CurriculumSummary> publishedCurricula(UUID programId);

    CurriculumSummary publishedCurriculumSummary(UUID curriculumId);

    AcademicCurriculumEntriesPage publishedCurriculumEntries(
            UUID curriculumId,
            CurriculumEntriesPageQuery query
    );

    AcademicCurriculumDraftsPage drafts(CurriculumDraftsPageQuery query);

    AcademicCurriculumDetails curriculum(UUID curriculumId);
}
