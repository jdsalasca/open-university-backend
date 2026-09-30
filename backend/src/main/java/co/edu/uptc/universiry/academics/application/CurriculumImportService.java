package co.edu.uptc.universiry.academics.application;

import co.edu.uptc.universiry.academics.domain.AcademicCatalogLimits;
import co.edu.uptc.universiry.academics.domain.AcademicCatalogValueRules;
import co.edu.uptc.universiry.academics.domain.AcademicLevel;
import co.edu.uptc.universiry.academics.domain.StudyModality;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.regex.Pattern;

public final class CurriculumImportService {

    private static final Pattern SEMESTER_NUMBER = Pattern.compile("^[0-9]+$");
    private static final Pattern CREDIT_NUMBER = Pattern.compile("^[0-9]{1,3}(?:\\.[0-9]{1,2})?$");
    private static final Pattern SOURCE_HASH = Pattern.compile("^[0-9a-f]{64}$");
    private static final Set<String> NORMALIZED_METADATA_CODES = Set.of(
            "program_code", "academic_level", "study_modality", "campus_code");

    private final CurriculumImportLimits limits;

    public CurriculumImportService(CurriculumImportLimits limits) {
        this.limits = Objects.requireNonNull(limits, "limits");
    }

    public ValidatedCurriculum validate(ParsedCurriculum parsed) {
        if (parsed == null || parsed.rows() == null || parsed.rows().isEmpty()) {
            throw invalidData(List.of());
        }
        if (parsed.rows().size() > limits.maxRows()) {
            throw new CurriculumCsvException(CurriculumCsvException.Code.TOO_MANY_ROWS, List.of());
        }

        LinkedHashSet<CurriculumCsvIssue> issues = new LinkedHashSet<>();
        if (!SOURCE_HASH.matcher(parsed.sourceSha256()).matches()) {
            addIssue(issues, null, null, CurriculumCsvIssue.Code.INVALID_VALUE);
        }

        Map<String, String> firstMetadata = null;
        ValidatedProgram program = null;
        String curriculumVersion = null;
        String cohortFrom = null;
        String cohortThrough = null;
        String approvalReference = null;
        List<ValidatedCurriculumEntry> entries = new ArrayList<>(parsed.rows().size());
        Set<String> subjectCodes = new HashSet<>();

        for (int index = 0; index < parsed.rows().size(); index++) {
            ParsedCurriculumRow row = parsed.rows().get(index);
            if (row == null || row.values() == null || row.rowNumber() < 2) {
                addIssue(issues, row == null ? null : row.rowNumber(), null, CurriculumCsvIssue.Code.INVALID_VALUE);
                continue;
            }
            Map<String, String> values = row.values();
            compareMetadata(values, firstMetadata, row.rowNumber(), issues);
            if (firstMetadata == null) {
                firstMetadata = canonicalMetadata(values);
            }

            String programCode = identifier(values, "program_code", AcademicCatalogLimits.MAX_PROGRAM_CODE_LENGTH,
                    row.rowNumber(), issues);
            AcademicLevel academicLevel = academicLevel(values, row.rowNumber(), issues);
            StudyModality studyModality = studyModality(values, row.rowNumber(), issues);
            String sniesCode = optionalText(values, "snies_code", AcademicCatalogLimits.MAX_SNIES_CODE_LENGTH,
                    row.rowNumber(), issues);
            String programName = requiredText(values, "program_name", AcademicCatalogLimits.MAX_PROGRAM_NAME_LENGTH,
                    row.rowNumber(), issues);
            String faculty = requiredText(values, "faculty", AcademicCatalogLimits.MAX_FACULTY_LENGTH,
                    row.rowNumber(), issues);
            String campusCode = identifier(values, "campus_code", AcademicCatalogLimits.MAX_CAMPUS_CODE_LENGTH,
                    row.rowNumber(), issues);
            String campusName = requiredText(values, "campus_name", AcademicCatalogLimits.MAX_CAMPUS_NAME_LENGTH,
                    row.rowNumber(), issues);
            String version = requiredText(values, "curriculum_version",
                    AcademicCatalogLimits.MAX_CURRICULUM_VERSION_LENGTH, row.rowNumber(), issues);
            CohortTermValue start = requiredCohortTerm(values, "cohort_from", row.rowNumber(), issues);
            CohortTermValue end = optionalCohortTerm(values, "cohort_through", row.rowNumber(), issues);
            String approval = requiredText(values, "approval_reference",
                    AcademicCatalogLimits.MAX_APPROVAL_REFERENCE_LENGTH, row.rowNumber(), issues);

            if (start != null && end != null && start.term() != null && end.term() != null
                    && end.term().compareTo(start.term()) < 0) {
                addIssue(issues, row.rowNumber(), "cohort_through", CurriculumCsvIssue.Code.INVALID_COHORT_RANGE);
            }

            if (program == null && programCode != null && academicLevel != null && studyModality != null
                    && programName != null && faculty != null && campusCode != null && campusName != null) {
                program = new ValidatedProgram(
                        programCode, academicLevel, studyModality, sniesCode,
                        programName, faculty, campusCode, campusName);
            }
            if (curriculumVersion == null && version != null && start != null && start.normalized() != null
                    && approval != null && end != null && (values.get("cohort_through") == null
                    || values.get("cohort_through").isBlank() || end.normalized() != null)) {
                curriculumVersion = version;
                cohortFrom = start.normalized();
                cohortThrough = end.normalized();
                approvalReference = approval;
            }

            int semester = semester(values, row.rowNumber(), issues);
            String subjectCode = identifier(values, "subject_code", AcademicCatalogLimits.MAX_SUBJECT_CODE_LENGTH,
                    row.rowNumber(), issues);
            String subjectName = requiredText(values, "subject_name", AcademicCatalogLimits.MAX_SUBJECT_NAME_LENGTH,
                    row.rowNumber(), issues);
            BigDecimal credits = credits(values, row.rowNumber(), issues);
            String formationSpace = requiredText(values, "formation_space",
                    AcademicCatalogLimits.MAX_FORMATION_SPACE_LENGTH, row.rowNumber(), issues);
            String component = requiredText(values, "component", AcademicCatalogLimits.MAX_COMPONENT_LENGTH,
                    row.rowNumber(), issues);
            String choiceGroup = optionalText(values, "choice_group",
                    AcademicCatalogLimits.MAX_CHOICE_GROUP_LENGTH, row.rowNumber(), issues);

            if (subjectCode != null && !subjectCodes.add(subjectCode)) {
                addIssue(issues, row.rowNumber(), "subject_code", CurriculumCsvIssue.Code.DUPLICATE_SUBJECT_CODE);
            }
            if (semester > 0 && subjectCode != null && subjectName != null && credits != null
                    && formationSpace != null && component != null) {
                entries.add(new ValidatedCurriculumEntry(
                        row.rowNumber(), index + 1, semester, subjectCode, subjectName,
                        credits, formationSpace, component, choiceGroup));
            }
        }

        if (!issues.isEmpty()) {
            throw invalidData(List.copyOf(issues));
        }
        return new ValidatedCurriculum(
                program, curriculumVersion, cohortFrom, cohortThrough,
                approvalReference, entries, parsed.sourceSha256());
    }

    private static void compareMetadata(
            Map<String, String> values,
            Map<String, String> firstMetadata,
            int rowNumber,
            Set<CurriculumCsvIssue> issues
    ) {
        if (firstMetadata == null) {
            return;
        }
        Map<String, String> currentMetadata = canonicalMetadata(values);
        for (String column : CurriculumCsvSchema.METADATA_HEADERS) {
            if (!Objects.equals(firstMetadata.get(column), currentMetadata.get(column))) {
                addIssue(issues, rowNumber, column, CurriculumCsvIssue.Code.INCONSISTENT_METADATA);
            }
        }
    }

    private static Map<String, String> canonicalMetadata(Map<String, String> values) {
        Map<String, String> metadata = new LinkedHashMap<>();
        for (String column : CurriculumCsvSchema.METADATA_HEADERS) {
            String value = values.get(column);
            if (value == null || value.isBlank()) {
                metadata.put(column, null);
            } else {
                String trimmed = value.strip();
                metadata.put(column, NORMALIZED_METADATA_CODES.contains(column)
                        ? trimmed.toUpperCase(Locale.ROOT)
                        : trimmed);
            }
        }
        return metadata;
    }

    private static String identifier(
            Map<String, String> values,
            String column,
            int maxLength,
            int rowNumber,
            Set<CurriculumCsvIssue> issues
    ) {
        String text = requiredText(values, column, maxLength, rowNumber, issues);
        if (text == null) {
            return null;
        }
        try {
            return AcademicCatalogValueRules.identifier(text, maxLength, column);
        } catch (IllegalArgumentException error) {
            addIssue(issues, rowNumber, column, CurriculumCsvIssue.Code.INVALID_IDENTIFIER);
            return null;
        }
    }

    private static String requiredText(
            Map<String, String> values,
            String column,
            int maxLength,
            int rowNumber,
            Set<CurriculumCsvIssue> issues
    ) {
        String value = values.get(column);
        if (value == null || value.isBlank()) {
            addIssue(issues, rowNumber, column, CurriculumCsvIssue.Code.REQUIRED_VALUE_MISSING);
            return null;
        }
        try {
            return AcademicCatalogValueRules.requiredText(value, maxLength, column);
        } catch (IllegalArgumentException error) {
            addIssue(issues, rowNumber, column, CurriculumCsvIssue.Code.FIELD_TOO_LONG);
            return null;
        }
    }

    private static String optionalText(
            Map<String, String> values,
            String column,
            int maxLength,
            int rowNumber,
            Set<CurriculumCsvIssue> issues
    ) {
        String value = values.get(column);
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return AcademicCatalogValueRules.optionalText(value, maxLength, column);
        } catch (IllegalArgumentException error) {
            addIssue(issues, rowNumber, column, CurriculumCsvIssue.Code.FIELD_TOO_LONG);
            return null;
        }
    }

    private static AcademicLevel academicLevel(
            Map<String, String> values,
            int rowNumber,
            Set<CurriculumCsvIssue> issues
    ) {
        String value = requiredText(values, "academic_level", AcademicCatalogLimits.MAX_ACADEMIC_LEVEL_LENGTH,
                rowNumber, issues);
        if (value == null) {
            return null;
        }
        if (AcademicLevel.PREGRADO.name().equals(value.toUpperCase(Locale.ROOT))) {
            return AcademicLevel.PREGRADO;
        }
        addIssue(issues, rowNumber, "academic_level", CurriculumCsvIssue.Code.UNSUPPORTED_ACADEMIC_LEVEL);
        return null;
    }

    private static StudyModality studyModality(
            Map<String, String> values,
            int rowNumber,
            Set<CurriculumCsvIssue> issues
    ) {
        String value = requiredText(values, "study_modality", AcademicCatalogLimits.MAX_STUDY_MODALITY_LENGTH,
                rowNumber, issues);
        if (value == null) {
            return null;
        }
        if (StudyModality.PRESENCIAL.name().equals(value.toUpperCase(Locale.ROOT))) {
            return StudyModality.PRESENCIAL;
        }
        addIssue(issues, rowNumber, "study_modality", CurriculumCsvIssue.Code.UNSUPPORTED_STUDY_MODALITY);
        return null;
    }

    private static CohortTermValue requiredCohortTerm(
            Map<String, String> values,
            String column,
            int rowNumber,
            Set<CurriculumCsvIssue> issues
    ) {
        String value = requiredText(values, column, AcademicCatalogLimits.MAX_COHORT_TERM_LENGTH,
                rowNumber, issues);
        return value == null ? null : cohortTerm(value, column, rowNumber, issues);
    }

    private static CohortTermValue optionalCohortTerm(
            Map<String, String> values,
            String column,
            int rowNumber,
            Set<CurriculumCsvIssue> issues
    ) {
        String value = optionalText(values, column, AcademicCatalogLimits.MAX_COHORT_TERM_LENGTH,
                rowNumber, issues);
        return value == null ? new CohortTermValue(null, null) : cohortTerm(value, column, rowNumber, issues);
    }

    private static CohortTermValue cohortTerm(
            String value,
            String column,
            int rowNumber,
            Set<CurriculumCsvIssue> issues
    ) {
        try {
            AcademicCatalogValueRules.CohortTerm term = AcademicCatalogValueRules.cohortTerm(value, column);
            return new CohortTermValue(String.format(Locale.ROOT, "%04d-%d", term.year(), term.term()), term);
        } catch (IllegalArgumentException error) {
            addIssue(issues, rowNumber, column, CurriculumCsvIssue.Code.INVALID_COHORT_RANGE);
            return null;
        }
    }

    private static int semester(Map<String, String> values, int rowNumber, Set<CurriculumCsvIssue> issues) {
        String value = values.get("semester");
        if (value == null || value.isBlank()) {
            addIssue(issues, rowNumber, "semester", CurriculumCsvIssue.Code.REQUIRED_VALUE_MISSING);
            return -1;
        }
        String normalized = value.strip();
        if (!SEMESTER_NUMBER.matcher(normalized).matches()) {
            addIssue(issues, rowNumber, "semester", CurriculumCsvIssue.Code.INVALID_SEMESTER);
            return -1;
        }
        try {
            int parsed = Integer.parseInt(normalized);
            if (parsed < 1 || parsed > AcademicCatalogLimits.MAX_SEMESTER_NUMBER) {
                addIssue(issues, rowNumber, "semester", CurriculumCsvIssue.Code.INVALID_SEMESTER);
                return -1;
            }
            return parsed;
        } catch (NumberFormatException error) {
            addIssue(issues, rowNumber, "semester", CurriculumCsvIssue.Code.INVALID_SEMESTER);
            return -1;
        }
    }

    private static BigDecimal credits(Map<String, String> values, int rowNumber, Set<CurriculumCsvIssue> issues) {
        String value = values.get("credits");
        if (value == null || value.isBlank()) {
            addIssue(issues, rowNumber, "credits", CurriculumCsvIssue.Code.REQUIRED_VALUE_MISSING);
            return null;
        }
        String normalized = value.strip();
        if (!CREDIT_NUMBER.matcher(normalized).matches()) {
            addIssue(issues, rowNumber, "credits", CurriculumCsvIssue.Code.INVALID_CREDITS);
            return null;
        }
        try {
            return AcademicCatalogValueRules.credits(new BigDecimal(normalized));
        } catch (IllegalArgumentException error) {
            addIssue(issues, rowNumber, "credits", CurriculumCsvIssue.Code.INVALID_CREDITS);
            return null;
        }
    }

    private static void addIssue(
            Set<CurriculumCsvIssue> issues,
            Integer rowNumber,
            String column,
            CurriculumCsvIssue.Code code
    ) {
        issues.add(new CurriculumCsvIssue(rowNumber, column, code));
    }

    private static CurriculumCsvException invalidData(List<CurriculumCsvIssue> issues) {
        return new CurriculumCsvException(CurriculumCsvException.Code.INVALID_DATA, issues);
    }

    private record CohortTermValue(String normalized, AcademicCatalogValueRules.CohortTerm term) {
    }
}
