package co.edu.uptc.universiry.academics.application;

import co.edu.uptc.universiry.academics.domain.AcademicCalendarActivity;

import java.util.List;

public record AcademicCalendarDraftCommand(String officialReference, List<AcademicCalendarActivity> activities) {
    public AcademicCalendarDraftCommand {
        activities = activities == null ? List.of() : List.copyOf(activities);
    }
}
