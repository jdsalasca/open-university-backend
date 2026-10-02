package co.edu.uptc.universiry.security.localpreview;

import org.springframework.security.oauth2.jwt.Jwt;

public interface LocalPreviewSessionService {
    LocalPreviewIssuedSession issue();

    Jwt decode(String token);

    void revoke(String token);
}
