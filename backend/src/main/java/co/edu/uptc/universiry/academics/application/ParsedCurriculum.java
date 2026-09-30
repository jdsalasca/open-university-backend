package co.edu.uptc.universiry.academics.application;

import java.util.List;
import java.util.Objects;

public record ParsedCurriculum(List<ParsedCurriculumRow> rows, String sourceSha256) {

    public ParsedCurriculum {
        rows = List.copyOf(rows);
        Objects.requireNonNull(sourceSha256, "sourceSha256");
    }
}
