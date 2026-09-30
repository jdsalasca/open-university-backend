package co.edu.uptc.universiry.security;

import org.springframework.core.convert.converter.Converter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;

import java.util.Collection;
import java.util.List;
import java.util.Set;

public final class BrandingAuthoritiesConverter implements Converter<Jwt, Collection<GrantedAuthority>> {

    private static final Set<String> ALLOWED_AUTHORITIES = Set.of("BRAND_ADMIN", "INSTITUTIONAL_ADMIN");

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
                .filter(ALLOWED_AUTHORITIES::contains)
                .distinct()
                .map(value -> (GrantedAuthority) new SimpleGrantedAuthority(value))
                .toList();
    }
}
