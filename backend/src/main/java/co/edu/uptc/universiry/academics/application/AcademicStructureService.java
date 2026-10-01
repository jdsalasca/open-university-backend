package co.edu.uptc.universiry.academics.application;

import co.edu.uptc.universiry.academics.domain.AcademicOrganizationUnit;
import co.edu.uptc.universiry.academics.domain.AcademicSite;
import co.edu.uptc.universiry.academics.domain.AcademicStructureSnapshot;

import java.util.UUID;

public interface AcademicStructureService {
    AcademicStructureSnapshot publicStructure();

    AcademicStructureSnapshot adminStructure();

    AcademicOrganizationUnit createUnit(AcademicOrganizationUnitCommand command, String actorSub);

    AcademicOrganizationUnit createChildUnit(UUID parentId, AcademicOrganizationChildUnitCommand command,
                                             String actorSub);

    AcademicSite createSite(AcademicSiteCommand command, String actorSub);

    void relateUnits(UUID parentId, UUID childId, AcademicStructureRelationCommand command, String actorSub);

    void closeOrganizationRelation(UUID parentId, UUID childId, AcademicStructureRelationCloseCommand command,
                                   String actorSub);

    void closeSiteRelation(UUID parentId, UUID childId, AcademicStructureRelationCloseCommand command,
                           String actorSub);

    void relateSites(UUID parentId, UUID childId, AcademicStructureRelationCommand command, String actorSub);

    void affiliateProgram(UUID programId, AcademicProgramAffiliationCommand command, String actorSub);

    UUID reassignProgramAffiliation(UUID programId, UUID affiliationId,
                                    AcademicProgramAffiliationReassignmentCommand command, String actorSub);

    void closeProgramAffiliation(UUID programId, UUID affiliationId,
                                 AcademicStructureRelationCloseCommand command, String actorSub);

    void changeOrganizationUnitOrder(UUID unitId, AcademicDisplayOrderCommand command, String actorSub);

    void changeSiteOrder(UUID siteId, AcademicDisplayOrderCommand command, String actorSub);

    void changeOrganizationRelationOrder(UUID parentId, UUID childId,
                                         AcademicDisplayOrderCommand command, String actorSub);

    void changeSiteRelationOrder(UUID parentId, UUID childId,
                                 AcademicDisplayOrderCommand command, String actorSub);

    void changeProgramAffiliationOrder(UUID programId, UUID affiliationId,
                                       AcademicDisplayOrderCommand command, String actorSub);
}
