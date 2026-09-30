package co.edu.uptc.universiry.branding.application;

public record Actor(String subject) {

    public Actor {
        if (subject == null || subject.isBlank()) {
            throw new IllegalArgumentException("An authenticated actor subject is required.");
        }
    }
}
