package co.edu.uptc.universiry.identity.infrastructure.web;

import co.edu.uptc.universiry.security.ApplicationPermission;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;

import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public record CurrentIdentityResponse(String subject, List<String> permissions) {

    static CurrentIdentityResponse from(Authentication authentication) {
        Set<String> granted = authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .collect(Collectors.toUnmodifiableSet());
        var permissions = Arrays.stream(ApplicationPermission.values())
                .map(ApplicationPermission::authority)
                .filter(granted::contains)
                .toList();
        return new CurrentIdentityResponse(authentication.getName(), permissions);
    }
}
