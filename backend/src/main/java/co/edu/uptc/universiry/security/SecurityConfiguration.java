package co.edu.uptc.universiry.security;

import co.edu.uptc.universiry.platform.i18n.application.MessageCatalog;
import co.edu.uptc.universiry.identity.application.RoleAssignmentRepository;
import jakarta.servlet.DispatcherType;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidatorResult;
import org.springframework.security.oauth2.jwt.BadJwtException;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtValidators;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.SupplierJwtDecoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.access.AccessDeniedHandler;
import tools.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.time.Clock;
import java.util.List;
import java.util.Locale;

import static org.springframework.http.HttpMethod.GET;
import static org.springframework.http.HttpMethod.POST;
import static org.springframework.http.HttpMethod.PATCH;
import static org.springframework.http.HttpMethod.PUT;
import static org.springframework.http.HttpStatus.FORBIDDEN;
import static org.springframework.http.HttpStatus.UNAUTHORIZED;

@Configuration(proxyBeanMethods = false)
public class SecurityConfiguration {

    private final MessageCatalog messages;
    private final ObjectMapper objectMapper;

    public SecurityConfiguration(MessageCatalog messages, ObjectMapper objectMapper) {
        this.messages = messages;
        this.objectMapper = objectMapper;
    }

    @Bean
    SecurityFilterChain applicationSecurity(
            HttpSecurity http,
            JwtAuthenticationConverter jwtAuthenticationConverter
    ) throws Exception {
        AuthenticationEntryPoint authenticationEntryPoint = this::writeUnauthorized;
        AccessDeniedHandler accessDeniedHandler = this::writeForbidden;

        http
                // Protected mutations require an explicit Authorization: Bearer token; authentication never uses cookies/sessions.
                .csrf(csrf -> csrf.disable())
                .authorizeHttpRequests(authorize -> authorize
                        .dispatcherTypeMatchers(DispatcherType.ERROR).permitAll()
                        .requestMatchers(
                                GET,
                                "/api/v1/branding",
                                "/api/v1/spaces",
                                "/api/v1/academic-catalog/programs",
                                "/api/v1/academic-structure",
                                "/api/v1/academic-periods",
                                "/api/v1/academic-catalog/curriculum-template",
                                "/api/v1/academic-catalog/programs/*/curricula",
                                "/api/v1/academic-catalog/curricula/*",
                                "/api/v1/academic-catalog/curricula/*/entries",
                                "/assets/**",
                                "/actuator/health",
                                "/actuator/health/**"
                        ).permitAll()
                        .requestMatchers(GET, "/api/v1/me").authenticated()
                        .requestMatchers(GET, "/api/v1/admin/branding")
                        .hasAuthority(ApplicationPermission.BRANDING_READ.authority())
                        .requestMatchers(PUT, "/api/v1/admin/branding")
                        .hasAuthority(ApplicationPermission.BRANDING_WRITE.authority())
                        .requestMatchers(POST,
                                "/api/v1/admin/branding/rollback",
                                "/api/v1/admin/branding/assets")
                        .hasAuthority(ApplicationPermission.BRANDING_WRITE.authority())
                        .requestMatchers(GET,
                                "/api/v1/admin/academic-catalog/drafts",
                                "/api/v1/admin/academic-catalog/curricula/*")
                        .hasAuthority(ApplicationPermission.ACADEMIC_CATALOG_READ.authority())
                        .requestMatchers(POST,
                                "/api/v1/admin/academic-catalog/imports",
                                "/api/v1/admin/academic-catalog/import-previews",
                                "/api/v1/admin/academic-catalog/curricula/*/publish")
                        .hasAuthority(ApplicationPermission.ACADEMIC_CATALOG_WRITE.authority())
                        .requestMatchers(GET, "/api/v1/admin/academic-periods")
                        .hasAuthority(ApplicationPermission.ACADEMIC_PERIOD_READ.authority())
                        .requestMatchers(GET, "/api/v1/admin/academic-periods/*/history")
                                .hasAuthority(ApplicationPermission.ACADEMIC_PERIOD_READ.authority())
                        .requestMatchers(GET,
                                "/api/v1/admin/academic-structure",
                                "/api/v1/admin/academic-structure/audit-events")
                        .hasAuthority(ApplicationPermission.ACADEMIC_STRUCTURE_READ.authority())
                        .requestMatchers(GET,
                                "/api/v1/admin/access/role-profiles",
                                "/api/v1/admin/access/identities",
                                "/api/v1/admin/access/assignments")
                        .hasAuthority(ApplicationPermission.IDENTITY_ROLES_READ.authority())
                        .requestMatchers(POST,
                                "/api/v1/admin/academic-periods",
                                "/api/v1/admin/academic-periods/*/calendars",
                                "/api/v1/admin/academic-periods/*/calendars/*/publish",
                                "/api/v1/admin/academic-periods/*/calendars/*/activate",
                                "/api/v1/admin/academic-periods/*/approve",
                                "/api/v1/admin/academic-periods/*/open",
                                "/api/v1/admin/academic-periods/*/close",
                                "/api/v1/admin/academic-periods/*/cancel")
                        .hasAuthority(ApplicationPermission.ACADEMIC_PERIOD_WRITE.authority())
                        .requestMatchers(POST,
                                "/api/v1/admin/academic-structure/units",
                                "/api/v1/admin/academic-structure/units/*/children",
                                "/api/v1/admin/academic-structure/sites",
                                "/api/v1/admin/academic-structure/units/*/children/*",
                                "/api/v1/admin/academic-structure/sites/*/children/*",
                                "/api/v1/admin/academic-structure/programs/*/affiliations",
                                "/api/v1/admin/academic-structure/programs/*/affiliations/*/reassign")
                        .hasAuthority(ApplicationPermission.ACADEMIC_STRUCTURE_WRITE.authority())
                        .requestMatchers(PATCH,
                                "/api/v1/admin/academic-structure/units/*/order",
                                "/api/v1/admin/academic-structure/sites/*/order",
                                "/api/v1/admin/academic-structure/units/*/children/*/order",
                                "/api/v1/admin/academic-structure/sites/*/children/*/order",
                                "/api/v1/admin/academic-structure/programs/*/affiliations/*/order",
                                "/api/v1/admin/academic-structure/units/*/children/*/close",
                                "/api/v1/admin/academic-structure/sites/*/children/*/close",
                                "/api/v1/admin/academic-structure/programs/*/affiliations/*/close")
                        .hasAuthority(ApplicationPermission.ACADEMIC_STRUCTURE_WRITE.authority())
                        .requestMatchers(POST, "/api/v1/admin/access/assignments")
                        .hasAuthority(ApplicationPermission.IDENTITY_ROLES_WRITE.authority())
                        .requestMatchers(PATCH, "/api/v1/admin/access/assignments/*/revoke")
                        .hasAuthority(ApplicationPermission.IDENTITY_ROLES_WRITE.authority())
                        .requestMatchers("/api/v1/admin/**").denyAll()
                        .anyRequest().denyAll()
                )
                .exceptionHandling(exceptions -> exceptions
                        .authenticationEntryPoint(authenticationEntryPoint)
                        .accessDeniedHandler(accessDeniedHandler)
                )
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .oauth2ResourceServer(resourceServer -> resourceServer
                        .authenticationEntryPoint(authenticationEntryPoint)
                        .jwt(jwt -> jwt.jwtAuthenticationConverter(jwtAuthenticationConverter))
                );

        return http.build();
    }

    @Bean
    JwtAuthenticationConverter jwtAuthenticationConverter(
            @Value("${UPTC_OIDC_AUTHORITIES_CLAIM:authorities}") String authoritiesClaim,
            @Value("${UPTC_OIDC_ROLE_PERMISSION_MAPPING:}") String rolePermissionMapping,
            RoleAssignmentRepository assignments,
            Clock clock
    ) {
        JwtAuthenticationConverter converter = new JwtAuthenticationConverter();
        converter.setJwtGrantedAuthoritiesConverter(
                new EffectiveAuthoritiesConverter(
                        new ApplicationAuthoritiesConverter(authoritiesClaim, rolePermissionMapping), assignments, clock));
        return converter;
    }

    @Bean
    JwtDecoder jwtDecoder(
            @Value("${UPTC_OIDC_ISSUER_URI:}") String issuerUri,
            @Value("${UPTC_OIDC_AUDIENCE:}") String audience
    ) {
        if (issuerUri.isBlank() || audience.isBlank()) {
            return token -> {
                throw new BadJwtException("Institutional OIDC issuer and audience are not configured.");
            };
        }

        return new SupplierJwtDecoder(() -> {
            NimbusJwtDecoder decoder = NimbusJwtDecoder.withIssuerLocation(issuerUri).build();
            OAuth2TokenValidator<Jwt> audienceValidator = jwt -> {
                if (jwt.getAudience().contains(audience)) {
                    return OAuth2TokenValidatorResult.success();
                }
                return OAuth2TokenValidatorResult.failure(new OAuth2Error(
                        "invalid_token",
                        "The token audience is not authorized for this service.",
                        null
                ));
            };
            decoder.setJwtValidator(JwtValidators.createDefaultWithValidators(
                    List.of(JwtValidators.createDefaultWithIssuer(issuerUri), audienceValidator)
            ));
            return decoder;
        });
    }

    private void writeUnauthorized(jakarta.servlet.http.HttpServletRequest request,
                                   jakarta.servlet.http.HttpServletResponse response,
                                   org.springframework.security.core.AuthenticationException exception) throws IOException {
        response.setHeader(HttpHeaders.WWW_AUTHENTICATE, "Bearer");
        writeError(response, UNAUTHORIZED.value(), "unauthorized",
                messages.message("security.unauthorized", requestLocale(request)));
    }

    private void writeForbidden(jakarta.servlet.http.HttpServletRequest request,
                                jakarta.servlet.http.HttpServletResponse response,
                                org.springframework.security.access.AccessDeniedException exception) throws IOException {
        writeError(response, FORBIDDEN.value(), "forbidden",
                messages.message("security.forbidden", requestLocale(request)));
    }

    private Locale requestLocale(jakarta.servlet.http.HttpServletRequest request) {
        String acceptLanguage = request.getHeader(HttpHeaders.ACCEPT_LANGUAGE);
        return acceptLanguage == null || acceptLanguage.isBlank()
                ? Locale.forLanguageTag("es-CO")
                : request.getLocale();
    }

    private void writeError(jakarta.servlet.http.HttpServletResponse response,
                            int status,
                            String code,
                            String message) throws IOException {
        response.setStatus(status);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");
        objectMapper.writeValue(response.getWriter(), new SecurityErrorResponse(code, message));
    }

    private record SecurityErrorResponse(String error, String message) {
    }
}
