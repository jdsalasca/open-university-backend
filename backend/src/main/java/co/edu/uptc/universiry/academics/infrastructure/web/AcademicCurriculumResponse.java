package co.edu.uptc.universiry.academics.infrastructure.web;

import co.edu.uptc.universiry.academics.application.CurriculumSummary;
import co.edu.uptc.universiry.academics.domain.AcademicCurriculumStatus;
import co.edu.uptc.universiry.academics.domain.AcademicLevel;
import co.edu.uptc.universiry.academics.domain.StudyModality;

import java.time.Instant;
import java.util.UUID;

public record AcademicCurriculumResponse(
        UUID id,
        UUID programId,
        String programCode,
        AcademicLevel academicLevel,
        StudyModality studyModality,
        String campusCode,
        String programName,
        String faculty,
        String campusName,
        String curriculumVersion,
        String cohortFrom,
        String cohortThrough,
        String approvalReference,
        AcademicCurriculumStatus status,
        int entryCount,
        Instant createdAt,
        Instant publishedAt
) {

    public static AcademicCurriculumResponse from(CurriculumSummary summary) {
        return new AcademicCurriculumResponse(
                summary.id(), summary.programId(), summary.programCode(), summary.academicLevel(),
                summary.studyModality(), summary.campusCode(), summary.programName(), summary.faculty(),
                summary.campusName(), summary.curriculumVersion(), summary.cohortFrom(), summary.cohortThrough(),
                summary.approvalReference(), summary.status(), summary.entryCount(), summary.createdAt(),
                summary.publishedAt());
    }
}
