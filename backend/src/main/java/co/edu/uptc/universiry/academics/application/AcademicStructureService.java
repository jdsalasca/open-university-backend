package co.edu.uptc.universiry.academics.application;

import co.edu.uptc.universiry.academics.domain.AcademicOrganizationUnit;
import co.edu.uptc.universiry.academics.domain.AcademicSite;
import co.edu.uptc.universiry.academics.domain.AcademicStructureSnapshot;

import java.util.UUID;

public interface AcademicStructureService {
    AcademicStructureSnapshot publicStructure();

    AcademicStructureSnapshot adminStructure();

    AcademicOrganizationUnit createUnit(AcademicOrganizationUnitCommand command, String actorSub);

    AcademicSite createSite(AcademicSiteCommand command, String actorSub);

    void relateUnits(UUID parentId, UUID childId, AcademicStructureRelationCommand command, String actorSub);

    void relateSites(UUID parentId, UUID childId, AcademicStructureRelationCommand command, String actorSub);

    void affiliateProgram(UUID programId, AcademicProgramAffiliationCommand command, String actorSub);
}
