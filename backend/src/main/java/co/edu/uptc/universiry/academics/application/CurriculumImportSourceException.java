package co.edu.uptc.universiry.academics.application;

public final class CurriculumImportSourceException extends RuntimeException {

    public CurriculumImportSourceException(Throwable cause) {
        super("The uploaded curriculum source could not be read.", cause);
    }
}
