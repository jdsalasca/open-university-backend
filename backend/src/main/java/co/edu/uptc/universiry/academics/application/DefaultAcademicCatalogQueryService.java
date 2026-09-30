package co.edu.uptc.universiry.academics.application;

import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
public class DefaultAcademicCatalogQueryService implements AcademicCatalogQueryService {

    private final AcademicCatalogRepository repository;

    public DefaultAcademicCatalogQueryService(AcademicCatalogRepository repository) {
        this.repository = repository;
    }

    @Override
    public List<AcademicProgramSummary> publishedPrograms() {
        return repository.listPublishedPrograms();
    }

    @Override
    public List<CurriculumSummary> publishedCurricula(UUID programId) {
        return repository.listPublishedCurricula(programId);
    }

    @Override
    public List<CurriculumSummary> drafts() {
        return repository.listDrafts();
    }

    @Override
    public AcademicCurriculumDetails curriculum(UUID curriculumId) {
        return repository.findCurriculum(curriculumId).orElseThrow(AcademicCatalogNotFoundException::new);
    }
}
