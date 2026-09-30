package co.edu.uptc.universiry.identity.infrastructure.web;

import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/me")
public class CurrentIdentityController {

    @GetMapping
    public ResponseEntity<CurrentIdentityResponse> currentIdentity(Authentication authentication) {
        return ResponseEntity.ok()
                .cacheControl(CacheControl.noStore())
                .body(CurrentIdentityResponse.from(authentication));
    }
}
