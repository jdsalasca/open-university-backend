package co.edu.uptc.universiry.academics.infrastructure.web;

import co.edu.uptc.universiry.academics.application.AcademicStructureService;
import co.edu.uptc.universiry.academics.domain.AcademicStructureSnapshot;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

import static org.springframework.http.HttpStatus.CREATED;

@RestController
public class AcademicStructureController {

    private final AcademicStructureService service;

    public AcademicStructureController(AcademicStructureService service) {
        this.service = service;
    }

    @GetMapping("/api/v1/academic-structure")
    public AcademicStructureSnapshot publicStructure() {
        return service.publicStructure();
    }

    @GetMapping("/api/v1/admin/academic-structure")
    public AcademicStructureSnapshot adminStructure() {
        return service.adminStructure();
    }

    @PostMapping("/api/v1/admin/academic-structure/units")
    public ResponseEntity<AcademicStructureMutationResponse> createUnit(
            @Valid @RequestBody CreateAcademicOrganizationUnitRequest request,
            Authentication authentication
    ) {
        var unit = service.createUnit(request.toCommand(), authentication.getName());
        return ResponseEntity.status(CREATED).body(new AcademicStructureMutationResponse(unit.id()));
    }

    @PostMapping("/api/v1/admin/academic-structure/units/{parentId}/children")
    public ResponseEntity<AcademicStructureMutationResponse> createChildUnit(
            @PathVariable UUID parentId,
            @Valid @RequestBody CreateAcademicOrganizationChildUnitRequest request,
            Authentication authentication
    ) {
        var unit = service.createChildUnit(parentId, request.toCommand(), authentication.getName());
        return ResponseEntity.status(CREATED).body(new AcademicStructureMutationResponse(unit.id()));
    }

    @PostMapping("/api/v1/admin/academic-structure/sites")
    public ResponseEntity<AcademicStructureMutationResponse> createSite(
            @Valid @RequestBody CreateAcademicSiteRequest request,
            Authentication authentication
    ) {
        var site = service.createSite(request.toCommand(), authentication.getName());
        return ResponseEntity.status(CREATED).body(new AcademicStructureMutationResponse(site.id()));
    }

    @PostMapping("/api/v1/admin/academic-structure/units/{parentId}/children/{childId}")
    public ResponseEntity<Void> relateUnits(
            @PathVariable UUID parentId,
            @PathVariable UUID childId,
            @Valid @RequestBody CreateAcademicStructureRelationRequest request,
            Authentication authentication
    ) {
        service.relateUnits(parentId, childId, request.toCommand(), authentication.getName());
        return ResponseEntity.status(CREATED).build();
    }

    @PatchMapping("/api/v1/admin/academic-structure/units/{parentId}/children/{childId}/close")
    public ResponseEntity<Void> closeOrganizationRelation(
            @PathVariable UUID parentId,
            @PathVariable UUID childId,
            @Valid @RequestBody CloseAcademicStructureRelationRequest request,
            Authentication authentication
    ) {
        service.closeOrganizationRelation(parentId, childId, request.toCommand(), authentication.getName());
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/api/v1/admin/academic-structure/sites/{parentId}/children/{childId}/close")
    public ResponseEntity<Void> closeSiteRelation(
            @PathVariable UUID parentId,
            @PathVariable UUID childId,
            @Valid @RequestBody CloseAcademicStructureRelationRequest request,
            Authentication authentication
    ) {
        service.closeSiteRelation(parentId, childId, request.toCommand(), authentication.getName());
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/api/v1/admin/academic-structure/sites/{parentId}/children/{childId}")
    public ResponseEntity<Void> relateSites(
            @PathVariable UUID parentId,
            @PathVariable UUID childId,
            @Valid @RequestBody CreateAcademicStructureRelationRequest request,
            Authentication authentication
    ) {
        service.relateSites(parentId, childId, request.toCommand(), authentication.getName());
        return ResponseEntity.status(CREATED).build();
    }

    @PostMapping("/api/v1/admin/academic-structure/programs/{programId}/affiliations")
    public ResponseEntity<AcademicStructureMutationResponse> affiliateProgram(
            @PathVariable UUID programId,
            @Valid @RequestBody CreateAcademicProgramAffiliationRequest request,
            Authentication authentication
    ) {
        service.affiliateProgram(programId, request.toCommand(), authentication.getName());
        return ResponseEntity.status(CREATED).body(new AcademicStructureMutationResponse(programId));
    }

    @PatchMapping("/api/v1/admin/academic-structure/programs/{programId}/affiliations/{affiliationId}/close")
    public ResponseEntity<Void> closeProgramAffiliation(
            @PathVariable UUID programId,
            @PathVariable UUID affiliationId,
            @Valid @RequestBody CloseAcademicStructureRelationRequest request,
            Authentication authentication
    ) {
        service.closeProgramAffiliation(programId, affiliationId, request.toCommand(), authentication.getName());
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/api/v1/admin/academic-structure/units/{unitId}/order")
    public ResponseEntity<Void> changeUnitOrder(
            @PathVariable UUID unitId,
            @Valid @RequestBody ChangeAcademicDisplayOrderRequest request,
            Authentication authentication
    ) {
        service.changeOrganizationUnitOrder(unitId, request.toCommand(), authentication.getName());
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/api/v1/admin/academic-structure/sites/{siteId}/order")
    public ResponseEntity<Void> changeSiteOrder(
            @PathVariable UUID siteId,
            @Valid @RequestBody ChangeAcademicDisplayOrderRequest request,
            Authentication authentication
    ) {
        service.changeSiteOrder(siteId, request.toCommand(), authentication.getName());
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/api/v1/admin/academic-structure/units/{parentId}/children/{childId}/order")
    public ResponseEntity<Void> changeOrganizationRelationOrder(
            @PathVariable UUID parentId,
            @PathVariable UUID childId,
            @Valid @RequestBody ChangeAcademicDisplayOrderRequest request,
            Authentication authentication
    ) {
        service.changeOrganizationRelationOrder(parentId, childId, request.toCommand(), authentication.getName());
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/api/v1/admin/academic-structure/sites/{parentId}/children/{childId}/order")
    public ResponseEntity<Void> changeSiteRelationOrder(
            @PathVariable UUID parentId,
            @PathVariable UUID childId,
            @Valid @RequestBody ChangeAcademicDisplayOrderRequest request,
            Authentication authentication
    ) {
        service.changeSiteRelationOrder(parentId, childId, request.toCommand(), authentication.getName());
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/api/v1/admin/academic-structure/programs/{programId}/affiliations/{affiliationId}/order")
    public ResponseEntity<Void> changeProgramAffiliationOrder(
            @PathVariable UUID programId,
            @PathVariable UUID affiliationId,
            @Valid @RequestBody ChangeAcademicDisplayOrderRequest request,
            Authentication authentication
    ) {
        service.changeProgramAffiliationOrder(programId, affiliationId, request.toCommand(), authentication.getName());
        return ResponseEntity.noContent().build();
    }
}
