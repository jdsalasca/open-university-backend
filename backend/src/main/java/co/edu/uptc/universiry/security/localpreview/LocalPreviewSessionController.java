package co.edu.uptc.universiry.security.localpreview;

import org.springframework.context.annotation.Profile;
import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

import static org.springframework.http.HttpStatus.TOO_MANY_REQUESTS;

@RestController
@Profile("local-preview")
public class LocalPreviewSessionController {

    private final LocalPreviewSessionService sessions;

    public LocalPreviewSessionController(LocalPreviewSessionService sessions) {
        this.sessions = sessions;
    }

    @PostMapping("/api/v1/dev/local-preview-session")
    public ResponseEntity<LocalPreviewSessionResponse> createSession() {
        LocalPreviewIssuedSession issued = sessions.issue();
        return ResponseEntity.ok()
                .cacheControl(CacheControl.noStore())
                .body(new LocalPreviewSessionResponse(issued.accessToken(), issued.expiresAt().getEpochSecond()));
    }

    @DeleteMapping("/api/v1/dev/local-preview-session")
    public ResponseEntity<Void> revokeSession(Authentication authentication) {
        String token = ((JwtAuthenticationToken) authentication).getToken().getTokenValue();
        sessions.revoke(token);
        return ResponseEntity.noContent().cacheControl(CacheControl.noStore()).build();
    }

    @ExceptionHandler(LocalPreviewSessionLimitException.class)
    public ResponseEntity<Void> sessionLimitReached() {
        return ResponseEntity.status(TOO_MANY_REQUESTS).cacheControl(CacheControl.noStore()).build();
    }

    public record LocalPreviewSessionResponse(String accessToken, long expiresAt) {
    }
}
