package co.edu.uptc.universiry.identity.domain;

import co.edu.uptc.universiry.security.ApplicationPermission;

import java.util.Arrays;
import java.util.List;
import java.util.Set;

public enum RoleProfile {
    APPLICANT("Aspirante", false, Set.of(), Set.of()),
    ADMITTED("Admitido", false, Set.of(), Set.of()),
    STUDENT("Estudiante", false, Set.of(), Set.of()),
    TEACHER("Docente", true, managementScopes(), Set.of()),
    ADMINISTRATIVE("Administrativo", true, managementScopes(), Set.of()),
    ADMISSIONS("Admisiones", true, managementScopes(), Set.of()),
    DIRECTIVE("Directivo", true, managementScopes(), Set.of()),
    ADMINISTRATOR("Administrador", true, Set.of(ScopeKind.UNIVERSITY), Set.of(
            ApplicationPermission.IDENTITY_ROLES_READ,
            ApplicationPermission.IDENTITY_ROLES_WRITE));

    private final String displayName;
    private final boolean manuallyAssignable;
    private final Set<ScopeKind> allowedScopeKinds;
    private final Set<ApplicationPermission> permissions;

    RoleProfile(
            String displayName,
            boolean manuallyAssignable,
            Set<ScopeKind> allowedScopeKinds,
            Set<ApplicationPermission> permissions) {
        this.displayName = displayName;
        this.manuallyAssignable = manuallyAssignable;
        this.allowedScopeKinds = Set.copyOf(allowedScopeKinds);
        this.permissions = Set.copyOf(permissions);
    }

    public String key() {
        return name();
    }

    public String displayName() {
        return displayName;
    }

    public boolean manuallyAssignable() {
        return manuallyAssignable;
    }

    public Set<ScopeKind> allowedScopeKinds() {
        return allowedScopeKinds;
    }

    public Set<ApplicationPermission> permissions() {
        return permissions;
    }

    public static List<RoleProfile> catalog() {
        return List.copyOf(Arrays.asList(values()));
    }

    public static RoleProfile fromKey(String key) {
        if (key == null) {
            throw new IllegalArgumentException("role profile key is required");
        }
        try {
            return valueOf(key);
        } catch (IllegalArgumentException exception) {
            throw new IllegalArgumentException("unknown role profile", exception);
        }
    }

    private static Set<ScopeKind> managementScopes() {
        return Set.of(ScopeKind.UNIVERSITY, ScopeKind.SITE, ScopeKind.FACULTY,
                ScopeKind.PROGRAM, ScopeKind.JOB_APPOINTMENT);
    }
}
