package co.edu.uptc.universiry.academics.application;

import co.edu.uptc.universiry.academics.domain.AcademicPeriodKind;

import java.time.LocalDate;

public record AcademicPeriodCreateCommand(
        String code,
        AcademicPeriodKind kind,
        int academicYear,
        int sequenceNumber,
        LocalDate startsOn,
        LocalDate endsOn
) {
}
