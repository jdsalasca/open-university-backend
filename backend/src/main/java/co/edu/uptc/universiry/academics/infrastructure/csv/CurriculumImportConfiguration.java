package co.edu.uptc.universiry.academics.infrastructure.csv;

import co.edu.uptc.universiry.academics.application.CurriculumCsvParser;
import co.edu.uptc.universiry.academics.application.CurriculumImportLimits;
import co.edu.uptc.universiry.academics.application.CurriculumImportService;
import co.edu.uptc.universiry.academics.domain.AcademicCatalogLimits;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration(proxyBeanMethods = false)
public class CurriculumImportConfiguration {

    @Bean
    CurriculumImportLimits curriculumImportLimits(
            @Value("${academic-catalog.import.max-file-bytes:" + AcademicCatalogLimits.MAX_IMPORT_BYTES + "}")
            int maxFileBytes,
            @Value("${academic-catalog.import.max-rows:" + AcademicCatalogLimits.MAX_IMPORT_ROWS + "}")
            int maxRows
    ) {
        return new CurriculumImportLimits(maxFileBytes, maxRows);
    }

    @Bean
    CurriculumCsvParser curriculumCsvParser(CurriculumImportLimits limits) {
        return new ApacheCommonsCurriculumCsvParser(limits);
    }

    @Bean
    CurriculumImportService curriculumImportService(CurriculumImportLimits limits) {
        return new CurriculumImportService(limits);
    }
}
