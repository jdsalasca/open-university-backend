package co.edu.uptc.universiry.academics.application;

import java.io.InputStream;
import java.util.UUID;

public interface CurriculumPublicationService {

    CurriculumSummary importCsv(InputStream source, String actorSub);

    AcademicCurriculumDetails publish(UUID curriculumId, String actorSub);
}
