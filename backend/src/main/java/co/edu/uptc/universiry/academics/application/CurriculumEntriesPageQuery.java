package co.edu.uptc.universiry.academics.application;

import co.edu.uptc.universiry.academics.domain.AcademicCatalogLimits;

public record CurriculumEntriesPageQuery(int page, int pageSize, String search, Integer semester) {

    public static final String DEFAULT_PAGE = "1";
    public static final String DEFAULT_PAGE_SIZE = "100";
    public static final int MAX_PAGE_SIZE = 100;
    public static final int MAX_SEARCH_CODE_POINTS = 120;

    public CurriculumEntriesPageQuery {
        if (page < 1 || pageSize < 1 || pageSize > MAX_PAGE_SIZE) {
            throw new InvalidCurriculumEntriesPageQueryException();
        }
        if (semester != null && (semester < 1 || semester > AcademicCatalogLimits.MAX_SEMESTER_NUMBER)) {
            throw new InvalidCurriculumEntriesPageQueryException();
        }
        if (search != null) {
            search = search.strip();
            if (search.codePointCount(0, search.length()) > MAX_SEARCH_CODE_POINTS) {
                throw new InvalidCurriculumEntriesPageQueryException();
            }
            if (search.isEmpty()) {
                search = null;
            }
        }
    }
}
