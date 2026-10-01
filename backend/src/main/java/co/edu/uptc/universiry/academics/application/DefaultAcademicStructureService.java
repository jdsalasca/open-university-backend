package co.edu.uptc.universiry.academics.application;

import co.edu.uptc.universiry.academics.domain.AcademicOrganizationRelation;
import co.edu.uptc.universiry.academics.domain.AcademicOrganizationUnit;
import co.edu.uptc.universiry.academics.domain.AcademicProgramAffiliation;
import co.edu.uptc.universiry.academics.domain.AcademicSite;
import co.edu.uptc.universiry.academics.domain.AcademicSiteRelation;
import co.edu.uptc.universiry.academics.domain.AcademicStructureSnapshot;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.util.UUID;

@Service
public class DefaultAcademicStructureService implements AcademicStructureService {

    private final AcademicStructureRepository repository;
    private final Clock clock;

    public DefaultAcademicStructureService(AcademicStructureRepository repository, Clock clock) {
        this.repository = repository;
        this.clock = clock;
    }

    @Override
    public AcademicStructureSnapshot publicStructure() {
        return repository.findPublicStructure(clock.instant().atZone(clock.getZone()).toLocalDate());
    }

    @Override
    public AcademicStructureSnapshot adminStructure() {
        return repository.findAdminStructure();
    }

    @Override
    public AcademicOrganizationUnit createUnit(AcademicOrganizationUnitCommand command, String actorSub) {
        String actor = AcademicCatalogActorSub.require(actorSub);
        AcademicOrganizationUnit unit = new AcademicOrganizationUnit(UUID.randomUUID(), command.code(), command.type(),
                command.displayName(), command.displayOrder(), command.validFrom(), command.validThrough());
        String reference = requiredReference(command.sourceReference());
        repository.createOrganizationUnit(unit, actor, reference);
        return unit;
    }

    @Override
    public AcademicOrganizationUnit createChildUnit(UUID parentId, AcademicOrganizationChildUnitCommand command,
                                                    String actorSub) {
        String actor = AcademicCatalogActorSub.require(actorSub);
        String reference = requiredReference(command.sourceReference());
        AcademicOrganizationUnit unit = new AcademicOrganizationUnit(UUID.randomUUID(), command.code(), command.type(),
                command.displayName(), 0, command.validFrom(), command.validThrough());
        AcademicOrganizationRelation relation = new AcademicOrganizationRelation(
                parentId, unit.id(), command.relationshipDisplayOrder(), command.validFrom(), command.validThrough());
        repository.createChildOrganizationUnit(unit, relation, actor, reference);
        return unit;
    }

    @Override
    public AcademicSite createSite(AcademicSiteCommand command, String actorSub) {
        String actor = AcademicCatalogActorSub.require(actorSub);
        AcademicSite site = AcademicSite.active(UUID.randomUUID(), command.code(), command.type(),
                command.displayName(), command.displayOrder(), command.validFrom(), command.validThrough());
        String reference = requiredReference(command.sourceReference());
        repository.createSite(site, actor, reference);
        return site;
    }

    @Override
    public void relateUnits(UUID parentId, UUID childId, AcademicStructureRelationCommand command, String actorSub) {
        String actor = AcademicCatalogActorSub.require(actorSub);
        AcademicOrganizationRelation relation = new AcademicOrganizationRelation(
                parentId, childId, command.displayOrder(), command.validFrom(), command.validThrough());
        repository.relateOrganizationUnits(relation, actor, requiredReference(command.sourceReference()));
    }

    @Override
    public void relateSites(UUID parentId, UUID childId, AcademicStructureRelationCommand command, String actorSub) {
        String actor = AcademicCatalogActorSub.require(actorSub);
        AcademicSiteRelation relation = new AcademicSiteRelation(
                parentId, childId, command.displayOrder(), command.validFrom(), command.validThrough());
        repository.relateSites(relation, actor, requiredReference(command.sourceReference()));
    }

    @Override
    public void affiliateProgram(UUID programId, AcademicProgramAffiliationCommand command, String actorSub) {
        String actor = AcademicCatalogActorSub.require(actorSub);
        AcademicProgramAffiliation affiliation = new AcademicProgramAffiliation(UUID.randomUUID(), programId,
                command.organizationUnitId(), command.siteId(), command.displayOrder(), command.validFrom(), command.validThrough(),
                command.sourceReference());
        repository.affiliateProgram(affiliation, actor);
    }

    @Override
    public void changeOrganizationUnitOrder(UUID unitId, AcademicDisplayOrderCommand command, String actorSub) {
        repository.changeOrganizationUnitOrder(unitId, command, AcademicCatalogActorSub.require(actorSub));
    }

    @Override
    public void changeSiteOrder(UUID siteId, AcademicDisplayOrderCommand command, String actorSub) {
        repository.changeSiteOrder(siteId, command, AcademicCatalogActorSub.require(actorSub));
    }

    @Override
    public void changeOrganizationRelationOrder(UUID parentId, UUID childId,
                                                AcademicDisplayOrderCommand command, String actorSub) {
        repository.changeOrganizationRelationOrder(parentId, childId, command,
                AcademicCatalogActorSub.require(actorSub));
    }

    @Override
    public void changeSiteRelationOrder(UUID parentId, UUID childId,
                                        AcademicDisplayOrderCommand command, String actorSub) {
        repository.changeSiteRelationOrder(parentId, childId, command,
                AcademicCatalogActorSub.require(actorSub));
    }

    @Override
    public void changeProgramAffiliationOrder(UUID programId, UUID affiliationId,
                                              AcademicDisplayOrderCommand command, String actorSub) {
        repository.changeProgramAffiliationOrder(programId, affiliationId, command,
                AcademicCatalogActorSub.require(actorSub));
    }

    private static String requiredReference(String value) {
        if (value == null || value.isBlank() || value.trim().length() > 240) {
            throw new IllegalArgumentException("academicStructure.sourceReference is invalid");
        }
        return value.trim();
    }
}
