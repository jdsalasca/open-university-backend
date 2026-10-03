package co.edu.uptc.universiry.library.domain;

import java.util.List;

/** One bibliographic record. The copy count is not stored here: each physical exemplar is a LibraryCopy. */
public record LibraryTitle(
        String titleId,
        String title,
        List<String> authors,
        String edition,
        Integer publicationYear,
        String sourceReference
) {

    private static final int MAX_AUTHORS = 20;

    public LibraryTitle {
        titleId = LibraryText.required(titleId, 36, "titleId");
        title = LibraryText.required(title, 240, "title");
        edition = LibraryText.required(edition, 80, "edition");
        sourceReference = LibraryText.required(sourceReference, 240, "sourceReference");
        List<String> copied = authors == null ? List.of() : authors.stream()
                .map(author -> LibraryText.required(author, 160, "author"))
                .toList();
        if (copied.isEmpty() || copied.size() > MAX_AUTHORS) {
            throw new IllegalArgumentException("a bibliographic record requires between 1 and 20 authors");
        }
        if (publicationYear != null && (publicationYear < 1450 || publicationYear > 2200)) {
            throw new IllegalArgumentException("a publication year outside 1450..2200 is rejected");
        }
        authors = copied;
    }
}