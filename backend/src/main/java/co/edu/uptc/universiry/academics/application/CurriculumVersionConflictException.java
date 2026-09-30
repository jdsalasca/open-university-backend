package co.edu.uptc.universiry.academics.application;

public final class CurriculumVersionConflictException extends RuntimeException {

    public CurriculumVersionConflictException() {
        super("A curriculum with this program and version already exists.");
    }
}
