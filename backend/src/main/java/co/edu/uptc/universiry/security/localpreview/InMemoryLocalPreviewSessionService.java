package co.edu.uptc.universiry.security.localpreview;

import org.springframework.security.oauth2.jwt.BadJwtException;
import org.springframework.security.oauth2.jwt.Jwt;

import java.security.SecureRandom;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;

public final class InMemoryLocalPreviewSessionService implements LocalPreviewSessionService {

    public static final String LOCAL_ISSUER = "https://local-preview.universiry.invalid";
    public static final String LOCAL_SUBJECT = "local-preview-developer";

    private static final int TOKEN_BYTES = 32;

    private final Clock clock;
    private final SecureRandom secureRandom;
    private final Duration sessionLifetime;
    private final int maximumActiveSessions;
    private final Map<String, SessionWindow> activeSessions = new HashMap<>();

    public InMemoryLocalPreviewSessionService(Clock clock,
                                              SecureRandom secureRandom,
                                              Duration sessionLifetime,
                                              int maximumActiveSessions) {
        if (clock == null || secureRandom == null || sessionLifetime == null
                || sessionLifetime.isNegative() || sessionLifetime.isZero()
                || maximumActiveSessions < 1) {
            throw new IllegalArgumentException("Local preview session configuration is invalid.");
        }
        this.clock = clock;
        this.secureRandom = secureRandom;
        this.sessionLifetime = sessionLifetime;
        this.maximumActiveSessions = maximumActiveSessions;
    }

    @Override
    public synchronized LocalPreviewIssuedSession issue() {
        Instant issuedAt = clock.instant();
        activeSessions.entrySet().removeIf(entry -> !entry.getValue().expiresAt().isAfter(issuedAt));
        if (activeSessions.size() >= maximumActiveSessions) throw new LocalPreviewSessionLimitException();

        byte[] bytes = new byte[TOKEN_BYTES];
        String token;
        do {
            secureRandom.nextBytes(bytes);
            token = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
        } while (activeSessions.containsKey(token));

        Instant expiresAt = issuedAt.plus(sessionLifetime);
        activeSessions.put(token, new SessionWindow(issuedAt, expiresAt));
        return new LocalPreviewIssuedSession(token, expiresAt);
    }

    @Override
    public synchronized Jwt decode(String token) {
        SessionWindow window = token == null ? null : activeSessions.get(token);
        if (window == null || !window.expiresAt().isAfter(clock.instant())) {
            if (token != null) activeSessions.remove(token);
            throw new BadJwtException("The local preview token is invalid or expired.");
        }
        return Jwt.withTokenValue(token)
                .header("alg", "none")
                .issuer(LOCAL_ISSUER)
                .subject(LOCAL_SUBJECT)
                .issuedAt(window.issuedAt())
                .expiresAt(window.expiresAt())
                .build();
    }

    @Override
    public synchronized void revoke(String token) {
        if (token != null) activeSessions.remove(token);
    }

    private record SessionWindow(Instant issuedAt, Instant expiresAt) {
    }
}
