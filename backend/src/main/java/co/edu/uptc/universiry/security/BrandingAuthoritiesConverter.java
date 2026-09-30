package co.edu.uptc.universiry.security;

import org.springframework.core.convert.converter.Converter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;

public final class BrandingAuthoritiesConverter implements Converter<Jwt, Collection<GrantedAuthority>> {

    private static final Set<ApplicationPermission> BRANDING_PERMISSIONS = Set.of(
            ApplicationPermission.BRANDING_READ,
            ApplicationPermission.BRANDING_WRITE
    );

    private static final Map<String, Set<ApplicationPermission>> ROLE_PERMISSIONS = Map.of(
            "BRAND_ADMIN", BRANDING_PERMISSIONS,
            "INSTITUTIONAL_ADMIN", BRANDING_PERMISSIONS
    );

    private final String authoritiesClaim;

    public BrandingAuthoritiesConverter(String authoritiesClaim) {
        if (authoritiesClaim == null || authoritiesClaim.isBlank()) {
            throw new IllegalArgumentException("The configured institutional authorities claim cannot be blank.");
        }
        this.authoritiesClaim = authoritiesClaim;
    }

    @Override
    public Collection<GrantedAuthority> convert(Jwt jwt) {
        Object rawClaim = jwt.getClaims().get(authoritiesClaim);
        if (!(rawClaim instanceof Collection<?> claimValues)) {
            return List.of();
        }

        return claimValues.stream()
                .filter(String.class::isInstance)
                .map(String.class::cast)
                .flatMap(role -> ROLE_PERMISSIONS.getOrDefault(role, Set.of()).stream())
                .map(ApplicationPermission::authority)
                .distinct()
                .sorted()
                .map(value -> (GrantedAuthority) new SimpleGrantedAuthority(value))
                .toList();
    }
}
