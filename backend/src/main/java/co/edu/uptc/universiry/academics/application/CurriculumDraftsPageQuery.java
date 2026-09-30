package co.edu.uptc.universiry.academics.application;

public record CurriculumDraftsPageQuery(int pageSize, CurriculumDraftCursor after) {

    public static final String DEFAULT_PAGE_SIZE = "25";
    public static final int MAX_PAGE_SIZE = 100;

    public CurriculumDraftsPageQuery {
        if (pageSize < 1 || pageSize > MAX_PAGE_SIZE) {
            throw new InvalidCurriculumDraftsPageQueryException();
        }
    }
}
