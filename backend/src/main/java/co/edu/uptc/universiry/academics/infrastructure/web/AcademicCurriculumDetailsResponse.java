package co.edu.uptc.universiry.academics.infrastructure.web;

import co.edu.uptc.universiry.academics.application.AcademicCurriculumDetails;

import java.util.List;

public record AcademicCurriculumDetailsResponse(
        AcademicCurriculumResponse curriculum,
        List<AcademicCurriculumEntryResponse> entries
) {

    public AcademicCurriculumDetailsResponse {
        entries = List.copyOf(entries);
    }

    public static AcademicCurriculumDetailsResponse from(AcademicCurriculumDetails details) {
        return new AcademicCurriculumDetailsResponse(
                AcademicCurriculumResponse.from(details.curriculum()),
                details.entries().stream().map(AcademicCurriculumEntryResponse::from).toList());
    }
}
