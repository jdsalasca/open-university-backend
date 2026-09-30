package co.edu.uptc.universiry.academics.application;

import java.util.Map;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Objects;

public record ParsedCurriculumRow(int rowNumber, Map<String, String> values) {

    public ParsedCurriculumRow {
        values = Collections.unmodifiableMap(new LinkedHashMap<>(Objects.requireNonNull(values, "values")));
    }
}
