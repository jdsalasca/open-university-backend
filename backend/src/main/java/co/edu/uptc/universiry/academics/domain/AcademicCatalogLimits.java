package co.edu.uptc.universiry.academics.domain;

import java.math.BigDecimal;

public final class AcademicCatalogLimits {

    public static final int MAX_IMPORT_BYTES = 2 * 1024 * 1024;
    public static final int MAX_IMPORT_ROWS = 10_000;
    public static final int MAX_CURRICULUM_ENTRIES = MAX_IMPORT_ROWS;

    public static final int MAX_PROGRAM_CODE_LENGTH = 64;
    public static final int MAX_SUBJECT_CODE_LENGTH = 64;
    public static final int MAX_CAMPUS_CODE_LENGTH = 64;
    public static final int MAX_ACADEMIC_LEVEL_LENGTH = 20;
    public static final int MAX_STUDY_MODALITY_LENGTH = 20;
    public static final int MAX_SNIES_CODE_LENGTH = 32;
    public static final int MAX_PROGRAM_NAME_LENGTH = 240;
    public static final int MAX_SUBJECT_NAME_LENGTH = 240;
    public static final int MAX_FACULTY_LENGTH = 160;
    public static final int MAX_CAMPUS_NAME_LENGTH = 160;
    public static final int MAX_CURRICULUM_VERSION_LENGTH = 80;
    public static final int MAX_COHORT_TERM_LENGTH = 6;
    public static final int MAX_APPROVAL_REFERENCE_LENGTH = 240;
    public static final int MAX_FORMATION_SPACE_LENGTH = 120;
    public static final int MAX_COMPONENT_LENGTH = 120;
    public static final int MAX_CHOICE_GROUP_LENGTH = 100;
    public static final int MAX_SEMESTER_NUMBER = Short.MAX_VALUE;
    public static final int MAX_CREDIT_SCALE = 2;
    public static final BigDecimal MAX_CREDITS = new BigDecimal("999.99");

    private AcademicCatalogLimits() {
    }
}
