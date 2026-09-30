package co.edu.uptc.universiry.security;

import tools.jackson.core.StreamReadFeature;
import tools.jackson.core.json.JsonFactory;
import tools.jackson.databind.DeserializationFeature;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;
import org.springframework.core.convert.converter.Converter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

public final class ApplicationAuthoritiesConverter implements Converter<Jwt, Collection<GrantedAuthority>> {

    private static final ObjectMapper STRICT_JSON = JsonMapper.builder(JsonFactory.builder()
            .enable(StreamReadFeature.STRICT_DUPLICATE_DETECTION)
            .build())
            .enable(DeserializationFeature.FAIL_ON_TRAILING_TOKENS)
            .build();
    private static final Set<String> PERMISSIONS = java.util.Arrays.stream(ApplicationPermission.values())
            .map(ApplicationPermission::authority)
            .collect(Collectors.toUnmodifiableSet());

    private final String authoritiesClaim;
    private final Map<String, Set<String>> rolePermissions;

    public ApplicationAuthoritiesConverter(String authoritiesClaim) {
        this(authoritiesClaim, "{}");
    }

    public ApplicationAuthoritiesConverter(String authoritiesClaim, String rolePermissionsJson) {
        if (authoritiesClaim == null || authoritiesClaim.isBlank()) {
            throw new IllegalArgumentException("The configured institutional authorities claim cannot be blank.");
        }
        this.authoritiesClaim = authoritiesClaim.trim();
        this.rolePermissions = parseRolePermissions(rolePermissionsJson);
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
                .flatMap(role -> rolePermissions.getOrDefault(role, Set.of()).stream())
                .distinct()
                .sorted()
                .map(value -> (GrantedAuthority) new SimpleGrantedAuthority(value))
                .toList();
    }

    private static Map<String, Set<String>> parseRolePermissions(String json) {
        String configuration = json == null ? "" : json.trim();
        if (configuration.isEmpty()) return Map.of();
        if (configuration.length() > 16_384) {
            throw new IllegalArgumentException("The institutional role permission mapping exceeds 16 KiB.");
        }

        JsonNode root;
        try {
            root = STRICT_JSON.readTree(configuration);
        } catch (Exception invalidJson) {
            throw new IllegalArgumentException("The institutional role permission mapping must be a JSON object.",
                    invalidJson);
        }
        if (root == null || !root.isObject()) {
            throw new IllegalArgumentException("The institutional role permission mapping must be a JSON object.");
        }

        Map<String, Set<String>> parsed = new LinkedHashMap<>();
        root.properties().forEach(entry -> {
            String role = entry.getKey();
            JsonNode roleValue = entry.getValue();
            if (role.isBlank() || !role.equals(role.trim()) || role.length() > 240) {
                throw new IllegalArgumentException("Institutional role values must be nonblank exact claim values.");
            }
            if (!roleValue.isArray() || roleValue.isEmpty()) {
                throw new IllegalArgumentException("Every mapped institutional role must have a nonempty permission list.");
            }

            List<String> values = StreamSupport.stream(roleValue.spliterator(), false)
                    .map(ApplicationAuthoritiesConverter::requireConfiguredPermission)
                    .toList();
            if (Set.copyOf(values).size() != values.size()) {
                throw new IllegalArgumentException("An institutional role cannot repeat a permission.");
            }
            parsed.put(role, Set.copyOf(values));
        });
        return Map.copyOf(parsed);
    }

    private static String requireConfiguredPermission(JsonNode value) {
        if (!value.isTextual()) {
            throw new IllegalArgumentException("Configured application permissions must be text values.");
        }
        String permission = value.textValue();
        if (!PERMISSIONS.contains(permission)) {
            throw new IllegalArgumentException("The institutional role mapping contains an unknown application permission.");
        }
        return permission;
    }
}
