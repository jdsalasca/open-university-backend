package co.edu.uptc.universiry.academics.application;

import java.util.List;
import java.util.Set;

public final class CurriculumCsvSchema {

    public static final List<String> HEADERS = List.of(
            "program_code",
            "academic_level",
            "study_modality",
            "snies_code",
            "program_name",
            "faculty",
            "campus_code",
            "campus_name",
            "curriculum_version",
            "cohort_from",
            "cohort_through",
            "approval_reference",
            "semester",
            "subject_code",
            "subject_name",
            "credits",
            "formation_space",
            "component",
            "choice_group"
    );
    public static final Set<String> HEADER_SET = Set.copyOf(HEADERS);
    public static final List<String> METADATA_HEADERS = List.copyOf(HEADERS.subList(0, 12));

    private CurriculumCsvSchema() {
    }
}
