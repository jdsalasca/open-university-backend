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

/** Maps the authenticated request into the domain principal and permissions. Public because more than one module
 *  reads the caller's own identity, and the mapping must not be duplicated per capability. */
public final class AuthenticatedPrincipalWebMapper {

    private AuthenticatedPrincipalWebMapper() {
    }

    public static AuthenticatedPrincipal from(Authentication authentication) {
        if (!(authentication instanceof JwtAuthenticationToken jwtAuthentication)) {
            throw new IllegalArgumentException("an OIDC authenticated principal is required");
        }
        var token = jwtAuthentication.getToken();
        if (token.getIssuer() == null) {
            throw new IllegalArgumentException("the OIDC issuer claim is required");
        }
        return new AuthenticatedPrincipal(token.getIssuer().toString(), token.getSubject());
    }

    public static Set<ApplicationPermission> applicationPermissions(Collection<? extends GrantedAuthority> authorities) {
        Set<String> granted = authorities.stream()
                .map(GrantedAuthority::getAuthority)
                .collect(Collectors.toUnmodifiableSet());
        return Arrays.stream(ApplicationPermission.values())
                .filter(permission -> granted.contains(permission.authority()))
                .collect(Collectors.toUnmodifiableSet());
    }
}
