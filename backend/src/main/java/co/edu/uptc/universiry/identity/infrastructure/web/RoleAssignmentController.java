package co.edu.uptc.universiry.identity.infrastructure.web;

import co.edu.uptc.universiry.identity.application.RoleAssignmentService;
import co.edu.uptc.universiry.identity.domain.AuthenticatedPrincipal;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/admin/access")
public class RoleAssignmentController {

    private final RoleAssignmentService roles;

    public RoleAssignmentController(RoleAssignmentService roles) {
        this.roles = roles;
    }

    @GetMapping("/role-profiles")
    public ResponseEntity<List<RoleProfileResponse>> roleProfiles() {
        List<RoleProfileResponse> response = roles.roleProfiles().stream()
                .map(RoleProfileResponse::from)
                .toList();
        return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(response);
    }

    @GetMapping("/identities")
    public ResponseEntity<List<IdentityDirectoryEntryResponse>> searchIdentities(
            @RequestParam String subjectPrefix,
            @RequestParam(defaultValue = "20") int limit) {
        List<IdentityDirectoryEntryResponse> response = roles.searchIdentities(subjectPrefix, limit).stream()
                .map(IdentityDirectoryEntryResponse::from)
                .toList();
        return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(response);
    }

    @GetMapping("/assignments")
    public ResponseEntity<List<RoleAssignmentResponse>> assignments(
            @RequestParam UUID userId) {
        List<RoleAssignmentResponse> response = roles.assignmentsFor(userId).stream()
                .map(RoleAssignmentResponse::from)
                .toList();
        return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(response);
    }

    @PostMapping("/assignments")
    public ResponseEntity<RoleAssignmentResponse> assign(
            Authentication authentication,
            @RequestBody CreateRoleAssignmentRequest request) {
        var created = roles.assign(AuthenticatedPrincipalWebMapper.from(authentication), request.toCommand());
        return ResponseEntity.status(HttpStatus.CREATED)
                .cacheControl(CacheControl.noStore())
                .body(RoleAssignmentResponse.from(created));
    }

    @PatchMapping("/assignments/{assignmentId}/revoke")
    public ResponseEntity<RoleAssignmentResponse> revoke(
            Authentication authentication,
            @PathVariable UUID assignmentId,
            @RequestBody RevokeRoleAssignmentRequest request) {
        var revoked = roles.revoke(AuthenticatedPrincipalWebMapper.from(authentication), assignmentId,
                request.expectedVersion(), request.sourceReference());
        return ResponseEntity.ok()
                .cacheControl(CacheControl.noStore())
                .body(RoleAssignmentResponse.from(revoked));
    }
}
