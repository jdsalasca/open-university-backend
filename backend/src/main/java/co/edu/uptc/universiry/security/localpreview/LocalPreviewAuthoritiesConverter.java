package co.edu.uptc.universiry.security.localpreview;

import co.edu.uptc.universiry.security.ApplicationPermission;
import org.springframework.core.convert.converter.Converter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;

import java.util.Collection;
import java.util.List;

public final class LocalPreviewAuthoritiesConverter implements Converter<Jwt, Collection<GrantedAuthority>> {

    @Override
    public Collection<GrantedAuthority> convert(Jwt jwt) {
        if (!InMemoryLocalPreviewSessionService.LOCAL_ISSUER.equals(
                jwt.getIssuer() == null ? null : jwt.getIssuer().toString())
                || !InMemoryLocalPreviewSessionService.LOCAL_SUBJECT.equals(jwt.getSubject())) {
            return List.of();
        }
        return java.util.Arrays.stream(ApplicationPermission.values())
                .map(ApplicationPermission::authority)
                .map(SimpleGrantedAuthority::new)
                .map(GrantedAuthority.class::cast)
                .toList();
    }
}
