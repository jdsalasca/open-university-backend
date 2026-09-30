package co.edu.uptc.universiry.academics.domain;

import java.util.List;

public record AcademicStructureSnapshot(
        List<AcademicOrganizationUnit> units,
        List<AcademicOrganizationRelation> organizationRelations,
        List<AcademicSite> sites,
        List<AcademicSiteRelation> siteRelations,
        List<AcademicProgramAffiliation> programAffiliations
) {
    public AcademicStructureSnapshot {
        units = List.copyOf(units);
        organizationRelations = List.copyOf(organizationRelations);
        sites = List.copyOf(sites);
        siteRelations = List.copyOf(siteRelations);
        programAffiliations = List.copyOf(programAffiliations);
    }

    public static AcademicStructureSnapshot empty() {
        return new AcademicStructureSnapshot(List.of(), List.of(), List.of(), List.of(), List.of());
    }
}
