package co.edu.uptc.universiry.security.localpreview;

import java.time.Instant;

public record LocalPreviewIssuedSession(String accessToken, Instant expiresAt) {
}
