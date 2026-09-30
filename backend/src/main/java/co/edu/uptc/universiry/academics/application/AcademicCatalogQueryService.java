package co.edu.uptc.universiry.academics.application;

import java.util.List;
import java.util.UUID;

public interface AcademicCatalogQueryService {

    List<AcademicProgramSummary> publishedPrograms();

    List<CurriculumSummary> publishedCurricula(UUID programId);

    List<CurriculumSummary> drafts();

    AcademicCurriculumDetails curriculum(UUID curriculumId);
}
