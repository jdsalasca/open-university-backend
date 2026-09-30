package co.edu.uptc.universiry.academics.application;

import java.io.InputStream;

public interface CurriculumCsvParser {

    ParsedCurriculum parse(InputStream input);
}
