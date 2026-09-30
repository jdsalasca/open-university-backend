package co.edu.uptc.universiry.academics.infrastructure.web;

import co.edu.uptc.universiry.academics.application.AcademicCatalogQueryService;
import co.edu.uptc.universiry.academics.application.AcademicCurriculumDetails;
import co.edu.uptc.universiry.academics.application.CurriculumEntriesPageQuery;
import co.edu.uptc.universiry.academics.application.CurriculumCsvSchema;
import co.edu.uptc.universiry.academics.application.InvalidCurriculumEntriesPageQueryException;
import co.edu.uptc.universiry.academics.application.CurriculumImportSourceException;
import co.edu.uptc.universiry.academics.application.CurriculumPublicationService;
import co.edu.uptc.universiry.academics.application.CurriculumSummary;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.UUID;

import static org.springframework.http.HttpStatus.CREATED;

@RestController
public class AcademicCatalogController {

    private final AcademicCatalogQueryService queryService;
    private final CurriculumPublicationService publicationService;

    public AcademicCatalogController(
            AcademicCatalogQueryService queryService,
            CurriculumPublicationService publicationService
    ) {
        this.queryService = queryService;
        this.publicationService = publicationService;
    }

    @GetMapping("/api/v1/academic-catalog/programs")
    public List<AcademicProgramResponse> publishedPrograms() {
        return queryService.publishedPrograms().stream().map(AcademicProgramResponse::from).toList();
    }

    @GetMapping(value = "/api/v1/academic-catalog/curriculum-template", produces = "text/csv")
    public ResponseEntity<byte[]> curriculumTemplate() {
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType("text/csv;charset=UTF-8"))
                .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.attachment()
                        .filename("academic-curriculum-template.csv")
                        .build()
                        .toString())
                .body(CurriculumCsvSchema.csvTemplate().getBytes(StandardCharsets.UTF_8));
    }

    @GetMapping("/api/v1/academic-catalog/programs/{programId}/curricula")
    public List<AcademicCurriculumResponse> publishedCurricula(@PathVariable UUID programId) {
        return queryService.publishedCurricula(programId).stream().map(AcademicCurriculumResponse::from).toList();
    }

    @GetMapping("/api/v1/academic-catalog/curricula/{curriculumId}")
    public AcademicCurriculumResponse publishedCurriculum(@PathVariable UUID curriculumId) {
        return AcademicCurriculumResponse.from(queryService.publishedCurriculumSummary(curriculumId));
    }

    @GetMapping("/api/v1/academic-catalog/curricula/{curriculumId}/entries")
    public AcademicCurriculumEntriesPageResponse publishedCurriculumEntries(
            @PathVariable UUID curriculumId,
            @RequestParam(defaultValue = CurriculumEntriesPageQuery.DEFAULT_PAGE) String page,
            @RequestParam(defaultValue = CurriculumEntriesPageQuery.DEFAULT_PAGE_SIZE) String pageSize,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String semester
    ) {
        CurriculumEntriesPageQuery query = new CurriculumEntriesPageQuery(
                parseQueryInteger(page),
                parseQueryInteger(pageSize),
                search,
                semester == null ? null : parseQueryInteger(semester));
        return AcademicCurriculumEntriesPageResponse.from(
                queryService.publishedCurriculumEntries(curriculumId, query));
    }

    @GetMapping("/api/v1/admin/academic-catalog/drafts")
    public List<AcademicCurriculumResponse> drafts() {
        return queryService.drafts().stream().map(AcademicCurriculumResponse::from).toList();
    }

    @GetMapping("/api/v1/admin/academic-catalog/curricula/{curriculumId}")
    public AcademicCurriculumDetailsResponse curriculum(@PathVariable UUID curriculumId) {
        return details(queryService.curriculum(curriculumId));
    }

    @PostMapping(path = "/api/v1/admin/academic-catalog/imports", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<AcademicCurriculumResponse> importCsv(
            @RequestPart("file") MultipartFile file,
            Authentication authentication
    ) {
        try (InputStream source = file.getInputStream()) {
            CurriculumSummary created = publicationService.importCsv(source, authentication.getName());
            return ResponseEntity.status(CREATED).body(AcademicCurriculumResponse.from(created));
        } catch (IOException error) {
            throw new CurriculumImportSourceException(error);
        }
    }

    @PostMapping("/api/v1/admin/academic-catalog/curricula/{curriculumId}/publish")
    public AcademicCurriculumDetailsResponse publish(
            @PathVariable UUID curriculumId,
            Authentication authentication
    ) {
        AcademicCurriculumDetails details = publicationService.publish(curriculumId, authentication.getName());
        return details(details);
    }

    private static AcademicCurriculumDetailsResponse details(AcademicCurriculumDetails details) {
        return AcademicCurriculumDetailsResponse.from(details);
    }

    private static int parseQueryInteger(String value) {
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException invalidInteger) {
            throw new InvalidCurriculumEntriesPageQueryException();
        }
    }
}
