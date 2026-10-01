package co.edu.uptc.universiry.academics.application;

import co.edu.uptc.universiry.academics.domain.AcademicOrganizationRelation;
import co.edu.uptc.universiry.academics.domain.AcademicOrganizationUnit;
import co.edu.uptc.universiry.academics.domain.AcademicProgramAffiliation;
import co.edu.uptc.universiry.academics.domain.AcademicSite;
import co.edu.uptc.universiry.academics.domain.AcademicSiteRelation;
import co.edu.uptc.universiry.academics.domain.AcademicStructureSnapshot;

import java.time.LocalDate;
import java.util.UUID;

public interface AcademicStructureRepository {
    AcademicStructureSnapshot findPublicStructure(LocalDate asOf);

    AcademicStructureSnapshot findAdminStructure();

    void createOrganizationUnit(AcademicOrganizationUnit unit, String actorSub, String sourceReference);

    void createChildOrganizationUnit(AcademicOrganizationUnit unit, AcademicOrganizationRelation relation,
                                     String actorSub, String sourceReference);

    void createSite(AcademicSite site, String actorSub, String sourceReference);

    void relateOrganizationUnits(AcademicOrganizationRelation relation, String actorSub, String sourceReference);

    void relateSites(AcademicSiteRelation relation, String actorSub, String sourceReference);

    void affiliateProgram(AcademicProgramAffiliation affiliation, String actorSub);

    void changeOrganizationUnitOrder(UUID unitId, AcademicDisplayOrderCommand command, String actorSub);

    void changeSiteOrder(UUID siteId, AcademicDisplayOrderCommand command, String actorSub);

    void changeOrganizationRelationOrder(UUID parentId, UUID childId,
                                         AcademicDisplayOrderCommand command, String actorSub);

    void changeSiteRelationOrder(UUID parentId, UUID childId,
                                 AcademicDisplayOrderCommand command, String actorSub);

    void changeProgramAffiliationOrder(UUID programId, UUID affiliationId,
                                       AcademicDisplayOrderCommand command, String actorSub);
}
