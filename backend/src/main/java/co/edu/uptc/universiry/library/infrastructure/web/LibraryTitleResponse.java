package co.edu.uptc.universiry.library.infrastructure.web;

import co.edu.uptc.universiry.library.domain.LibraryTitle;

import java.util.List;

public record LibraryTitleResponse(
        String titleId,
        String title,
        List<String> authors,
        String edition,
        Integer publicationYear,
        String sourceReference
) {

    public static LibraryTitleResponse from(LibraryTitle title) {
        return new LibraryTitleResponse(title.titleId(), title.title(), title.authors(), title.edition(),
                title.publicationYear(), title.sourceReference());
    }
}