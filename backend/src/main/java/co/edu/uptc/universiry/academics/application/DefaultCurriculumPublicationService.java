package co.edu.uptc.universiry.academics.application;

import org.springframework.stereotype.Service;

import java.io.InputStream;
import java.util.Objects;
import java.util.UUID;

@Service
public class DefaultCurriculumPublicationService implements CurriculumPublicationService {

    private final CurriculumCsvParser parser;
    private final CurriculumImportService importService;
    private final AcademicCatalogRepository repository;

    public DefaultCurriculumPublicationService(
            CurriculumCsvParser parser,
            CurriculumImportService importService,
            AcademicCatalogRepository repository
    ) {
        this.parser = parser;
        this.importService = importService;
        this.repository = repository;
    }

    @Override
    public CurriculumImportPreview previewCsv(InputStream source, String actorSub) {
        AcademicCatalogActorSub.require(actorSub);
        ParsedCurriculum parsed = parser.parse(Objects.requireNonNull(source, "source"));
        ValidatedCurriculum validated = importService.validate(parsed);
        CurriculumVersionComparison comparison = repository
                .findLatestPublishedCurriculum(CurriculumProgramIdentity.from(validated.program()))
                .map(reference -> CurriculumVersionComparison.compare(validated, reference))
                .orElseGet(CurriculumVersionComparison::noReference);
        return CurriculumImportPreview.from(validated, comparison);
    }

    @Override
    public CurriculumSummary importCsv(InputStream source, String actorSub) {
        String actor = AcademicCatalogActorSub.require(actorSub);
        ParsedCurriculum parsed = parser.parse(Objects.requireNonNull(source, "source"));
        ValidatedCurriculum validated = importService.validate(parsed);
        return repository.createDraft(validated, actor);
    }

    @Override
    public AcademicCurriculumDetails publish(UUID curriculumId, String actorSub) {
        String actor = AcademicCatalogActorSub.require(actorSub);
        CurriculumPublishResult result = repository.publishDraft(curriculumId, actor);
        return switch (result) {
            case PUBLISHED -> repository.findCurriculum(curriculumId)
                    .orElseThrow(AcademicCatalogNotFoundException::new);
            case NOT_FOUND -> throw new AcademicCatalogNotFoundException();
            case CONFLICT -> throw new CurriculumVersionConflictException();
        };
    }
}
