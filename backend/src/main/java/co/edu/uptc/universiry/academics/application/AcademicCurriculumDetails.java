package co.edu.uptc.universiry.academics.application;

import java.util.List;

public record AcademicCurriculumDetails(CurriculumSummary curriculum, List<AcademicCurriculumEntrySummary> entries) {
}
