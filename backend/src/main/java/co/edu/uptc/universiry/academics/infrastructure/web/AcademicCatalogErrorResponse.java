package co.edu.uptc.universiry.academics.infrastructure.web;

import java.util.List;

public record AcademicCatalogErrorResponse(String error, String message, List<AcademicCatalogIssueResponse> issues) {

    public AcademicCatalogErrorResponse {
        issues = List.copyOf(issues);
    }

    public static AcademicCatalogErrorResponse of(String error, String message) {
        return new AcademicCatalogErrorResponse(error, message, List.of());
    }
}
