package co.edu.uptc.universiry.academics.application;

import java.util.List;

public record AcademicOfferingDraftPage(List<AcademicOfferingDraftView> drafts, String nextCursor) {
    public AcademicOfferingDraftPage {
        drafts = List.copyOf(drafts);
    }
}
