package co.edu.uptc.universiry.identity.infrastructure.web;

import co.edu.uptc.universiry.identity.domain.AuthenticatedPrincipal;
import co.edu.uptc.universiry.security.ApplicationPermission;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

import java.util.Arrays;
import java.util.Collection;
import java.util.Set;
import java.util.stream.Collectors;

final class AuthenticatedPrincipalWebMapper {

    private AuthenticatedPrincipalWebMapper() {
    }

    static AuthenticatedPrincipal from(Authentication authentication) {
        if (!(authentication instanceof JwtAuthenticationToken jwtAuthentication)) {
            throw new IllegalArgumentException("an OIDC authenticated principal is required");
        }
        var token = jwtAuthentication.getToken();
        if (token.getIssuer() == null) {
            throw new IllegalArgumentException("the OIDC issuer claim is required");
        }
        return new AuthenticatedPrincipal(token.getIssuer().toString(), token.getSubject());
    }

    static Set<ApplicationPermission> applicationPermissions(Collection<? extends GrantedAuthority> authorities) {
        Set<String> granted = authorities.stream()
                .map(GrantedAuthority::getAuthority)
                .collect(Collectors.toUnmodifiableSet());
        return Arrays.stream(ApplicationPermission.values())
                .filter(permission -> granted.contains(permission.authority()))
                .collect(Collectors.toUnmodifiableSet());
    }
}
