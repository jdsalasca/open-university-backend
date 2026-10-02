package co.edu.uptc.universiry.academics.infrastructure.web;

import co.edu.uptc.universiry.academics.application.AcademicOfferingDraftService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

import static org.springframework.http.HttpStatus.CREATED;

@RestController
public class AcademicOfferingDraftController {

    private final AcademicOfferingDraftService service;

    public AcademicOfferingDraftController(AcademicOfferingDraftService service) {
        this.service = service;
    }

    @GetMapping("/api/v1/admin/academic-offerings")
    public AcademicOfferingDraftPageResponse drafts(
            @RequestParam UUID periodId,
            @RequestParam(defaultValue = "25") int limit,
            @RequestParam(required = false) String before
    ) {
        return AcademicOfferingDraftPageResponse.from(service.drafts(periodId, limit, before));
    }

    @GetMapping("/api/v1/admin/academic-offerings/{offeringId}/audit-events")
    public AcademicOfferingAuditPageResponse history(
            @PathVariable UUID offeringId,
            @RequestParam(defaultValue = "25") int limit,
            @RequestParam(required = false) String before
    ) {
        return AcademicOfferingAuditPageResponse.from(service.history(offeringId, limit, before));
    }

    @PostMapping("/api/v1/admin/academic-offerings")
    public ResponseEntity<AcademicOfferingDraftMutationResponse> create(
            @Valid @RequestBody CreateAcademicOfferingDraftRequest request,
            Authentication authentication
    ) {
        var created = service.create(request.toCommand(), authentication.getName());
        return ResponseEntity.status(CREATED).body(AcademicOfferingDraftMutationResponse.from(created));
    }

    @PutMapping("/api/v1/admin/academic-offerings/{offeringId}")
    public AcademicOfferingDraftMutationResponse update(
            @PathVariable UUID offeringId,
            @Valid @RequestBody UpdateAcademicOfferingDraftRequest request,
            Authentication authentication
    ) {
        return AcademicOfferingDraftMutationResponse.from(
                service.update(offeringId, request.toCommand(), authentication.getName()));
    }
}
