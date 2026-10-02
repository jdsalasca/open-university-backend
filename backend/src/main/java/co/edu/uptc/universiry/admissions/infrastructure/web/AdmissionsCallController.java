package co.edu.uptc.universiry.admissions.infrastructure.web;

import co.edu.uptc.universiry.admissions.application.AdmissionsCallService;
import co.edu.uptc.universiry.identity.domain.AuthenticatedPrincipal;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

import static org.springframework.http.HttpStatus.CREATED;

@RestController
public class AdmissionsCallController {

    private final AdmissionsCallService service;

    public AdmissionsCallController(AdmissionsCallService service) {
        this.service = service;
    }

    @GetMapping("/api/v1/admissions/calls")
    public List<PublicAdmissionsCallResponse> publicCalls() {
        return service.publicCalls().stream().map(PublicAdmissionsCallResponse::from).toList();
    }

    @GetMapping("/api/v1/admin/admissions/calls")
    public List<AdmissionsCallAdminResponse> adminCalls() {
        return service.adminCalls().stream().map(AdmissionsCallAdminResponse::from).toList();
    }

    @GetMapping("/api/v1/admin/admissions/calls/{callId}")
    public AdmissionsCallAdminResponse adminCall(@PathVariable UUID callId) {
        return AdmissionsCallAdminResponse.from(service.adminCall(callId));
    }

    @PostMapping("/api/v1/admin/admissions/calls")
    public ResponseEntity<AdmissionsCallAdminResponse> createCall(
            @Valid @RequestBody CreateAdmissionsCallRequest request,
            Authentication authentication
    ) {
        return ResponseEntity.status(CREATED).body(AdmissionsCallAdminResponse.from(
                service.createCall(request.callKey(), request.content(), principal(authentication))));
    }

    @PostMapping("/api/v1/admin/admissions/calls/{callId}/revisions")
    public ResponseEntity<AdmissionsCallRevisionResponse> createRevision(
            @PathVariable UUID callId,
            @Valid @RequestBody CreateAdmissionsCallRevisionRequest request,
            Authentication authentication
    ) {
        return ResponseEntity.status(CREATED).body(AdmissionsCallRevisionResponse.from(
                service.createRevision(callId, request.content(), principal(authentication))));
    }

    @PutMapping("/api/v1/admin/admissions/calls/{callId}/revisions/{revisionId}")
    public AdmissionsCallRevisionResponse updateDraft(
            @PathVariable UUID callId,
            @PathVariable UUID revisionId,
            @Valid @RequestBody UpdateAdmissionsCallDraftRequest request,
            Authentication authentication
    ) {
        return AdmissionsCallRevisionResponse.from(service.updateDraft(callId, revisionId,
                request.expectedDraftVersion(), request.content(), principal(authentication)));
    }

    @PostMapping("/api/v1/admin/admissions/calls/{callId}/revisions/{revisionId}/publish")
    public AdmissionsCallRevisionResponse publish(
            @PathVariable UUID callId,
            @PathVariable UUID revisionId,
            @Valid @RequestBody PublishAdmissionsCallRevisionRequest request,
            Authentication authentication
    ) {
        return AdmissionsCallRevisionResponse.from(service.publish(callId, revisionId,
                request.expectedDraftVersion(), request.expectedPublishedRevisionId(), request.officialReference(),
                principal(authentication)));
    }

    private static AuthenticatedPrincipal principal(Authentication authentication) {
        if (!(authentication instanceof JwtAuthenticationToken token) || token.getToken().getIssuer() == null) {
            throw new co.edu.uptc.universiry.admissions.application.AdmissionsActorException();
        }
        try {
            return new AuthenticatedPrincipal(token.getToken().getIssuer().toString(), token.getToken().getSubject());
        } catch (IllegalArgumentException invalidPrincipal) {
            throw new co.edu.uptc.universiry.admissions.application.AdmissionsActorException();
        }
    }
}
