package co.edu.uptc.universiry.academics.application;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface AcademicCatalogRepository {

    CurriculumSummary createDraft(ValidatedCurriculum curriculum, String actorSub);

    Optional<AcademicCurriculumDetails> findCurriculum(UUID curriculumId);

    List<AcademicProgramSummary> listPublishedPrograms();

    List<CurriculumSummary> listPublishedCurricula(UUID programId);

    List<CurriculumSummary> listDrafts();

    CurriculumPublishResult publishDraft(UUID curriculumId, String actorSub);
}
