package co.edu.uptc.universiry.academics.application;

import co.edu.uptc.universiry.academics.domain.AcademicCurriculumStatus;
import co.edu.uptc.universiry.academics.domain.AcademicLevel;
import co.edu.uptc.universiry.academics.domain.StudyModality;

import java.time.Instant;
import java.util.UUID;

public record CurriculumSummary(
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
        String sourceSha256,
        Instant createdAt,
        String createdBy,
        String publishedBy,
        Instant publishedAt
) {
}
