package co.edu.uptc.universiry.academics.infrastructure.web;

import co.edu.uptc.universiry.academics.application.CurriculumCsvIssue;

import java.util.Locale;

public record AcademicCatalogIssueResponse(Integer rowNumber, String column, String code) {

    public static AcademicCatalogIssueResponse from(CurriculumCsvIssue issue) {
        return new AcademicCatalogIssueResponse(
                issue.rowNumber(), issue.column(), issue.code().name().toLowerCase(Locale.ROOT));
    }
}
