package co.edu.uptc.universiry.academics.infrastructure.web;

import co.edu.uptc.universiry.academics.application.AcademicStructureService;
import co.edu.uptc.universiry.academics.domain.AcademicStructureSnapshot;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
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
}
