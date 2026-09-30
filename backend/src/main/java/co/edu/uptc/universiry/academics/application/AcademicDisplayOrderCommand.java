package co.edu.uptc.universiry.academics.application;

public record AcademicDisplayOrderCommand(
        int expectedDisplayOrder,
        int displayOrder,
        String sourceReference
) {
    public AcademicDisplayOrderCommand {
        if (expectedDisplayOrder < 0 || displayOrder < 0 || displayOrder > 100_000) {
            throw new IllegalArgumentException("academicStructure.displayOrder is invalid");
        }
        sourceReference = sourceReference == null ? "" : sourceReference.trim();
        if (sourceReference.isEmpty() || sourceReference.length() > 240) {
            throw new IllegalArgumentException("academicStructure.sourceReference is invalid");
        }
    }
}
