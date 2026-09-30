package co.edu.uptc.universiry.academics.domain;

public final class AcademicPeriodStateConflictException extends RuntimeException {
    public AcademicPeriodStateConflictException() {
        super("academic period transition is not valid for the current state");
    }
}
