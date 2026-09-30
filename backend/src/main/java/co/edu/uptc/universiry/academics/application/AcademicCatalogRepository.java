package co.edu.uptc.universiry.academics.application;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface AcademicCatalogRepository {

    CurriculumSummary createDraft(ValidatedCurriculum curriculum, String actorSub);

    Optional<AcademicCurriculumDetails> findCurriculum(UUID curriculumId);

    Optional<CurriculumSummary> findPublishedCurriculumSummary(UUID curriculumId);

    Optional<AcademicCurriculumEntriesPage> findPublishedCurriculumEntries(
            UUID curriculumId,
            CurriculumEntriesPageQuery query
    );

    List<AcademicProgramSummary> listPublishedPrograms();

    List<CurriculumSummary> listPublishedCurricula(UUID programId);

    AcademicCurriculumDraftsPage listDrafts(CurriculumDraftsPageQuery query);

    CurriculumPublishResult publishDraft(UUID curriculumId, String actorSub);
}
