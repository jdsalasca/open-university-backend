package co.edu.uptc.universiry.identity.infrastructure.web;

import co.edu.uptc.universiry.identity.application.CurrentIdentityService;
import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/me")
public class CurrentIdentityController {

    private final CurrentIdentityService currentIdentity;

    public CurrentIdentityController(CurrentIdentityService currentIdentity) {
        this.currentIdentity = currentIdentity;
    }

    @GetMapping
    public ResponseEntity<CurrentIdentityResponse> currentIdentity(Authentication authentication) {
        var principal = AuthenticatedPrincipalWebMapper.from(authentication);
        var permissions = AuthenticatedPrincipalWebMapper.applicationPermissions(authentication.getAuthorities());
        return ResponseEntity.ok()
                .cacheControl(CacheControl.noStore())
                .body(CurrentIdentityResponse.from(currentIdentity.currentIdentity(principal, permissions)));
    }
}
