package co.edu.uptc.universiry.academics.application;

public final class AcademicCatalogActorException extends IllegalArgumentException {

    public AcademicCatalogActorException() {
        super("The authenticated actor subject is invalid.");
    }
}
