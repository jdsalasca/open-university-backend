package co.edu.uptc.universiry.security;

import co.edu.uptc.universiry.identity.application.RoleAssignmentRepository;
import co.edu.uptc.universiry.identity.domain.AuthenticatedPrincipal;
import org.springframework.core.convert.converter.Converter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;

import java.time.Clock;
import java.time.LocalDate;
import java.util.Collection;
import java.util.Set;
import java.util.TreeSet;

public final class EffectiveAuthoritiesConverter implements Converter<Jwt, Collection<GrantedAuthority>> {

    private final ApplicationAuthoritiesConverter configuredClaims;
    private final RoleAssignmentRepository assignments;
    private final Clock clock;

    public EffectiveAuthoritiesConverter(
            ApplicationAuthoritiesConverter configuredClaims,
            RoleAssignmentRepository assignments,
            Clock clock) {
        this.configuredClaims = configuredClaims;
        this.assignments = assignments;
        this.clock = clock;
    }

    @Override
    public Collection<GrantedAuthority> convert(Jwt jwt) {
        Set<String> authorityNames = new TreeSet<>(configuredClaims.convert(jwt).stream()
                .map(GrantedAuthority::getAuthority)
                .toList());
        if (jwt.getIssuer() == null || jwt.getSubject() == null || jwt.getSubject().isBlank()) {
            return toAuthorities(authorityNames);
        }
        AuthenticatedPrincipal principal;
        try {
            principal = new AuthenticatedPrincipal(jwt.getIssuer().toString(), jwt.getSubject());
        } catch (IllegalArgumentException invalidIdentity) {
            return toAuthorities(authorityNames);
        }
        assignments.findActiveAssignments(principal, LocalDate.now(clock)).stream()
                .flatMap(assignment -> assignment.profile().permissions().stream())
                .map(ApplicationPermission::authority)
                .forEach(authorityNames::add);
        return toAuthorities(authorityNames);
    }

    private static Collection<GrantedAuthority> toAuthorities(Set<String> values) {
        return values.stream()
                .map(SimpleGrantedAuthority::new)
                .map(GrantedAuthority.class::cast)
                .toList();
    }
}
