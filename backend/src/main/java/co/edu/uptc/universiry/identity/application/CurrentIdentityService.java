package co.edu.uptc.universiry.identity.application;

import co.edu.uptc.universiry.identity.domain.AuthenticatedPrincipal;
import co.edu.uptc.universiry.identity.domain.RoleAssignment;
import co.edu.uptc.universiry.security.ApplicationPermission;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDate;
import java.util.Collection;
import java.util.Set;
import java.util.TreeSet;

@Service
public class CurrentIdentityService {

    private final IdentityDirectory identities;
    private final RoleAssignmentRepository assignments;
    private final Clock clock;

    public CurrentIdentityService(IdentityDirectory identities, RoleAssignmentRepository assignments, Clock clock) {
        this.identities = identities;
        this.assignments = assignments;
        this.clock = clock;
    }

    @Transactional
    public CurrentIdentitySnapshot currentIdentity(
            AuthenticatedPrincipal principal,
            Collection<ApplicationPermission> externallyGrantedPermissions) {
        if (principal == null || externallyGrantedPermissions == null) {
            throw new IllegalArgumentException("authenticated principal and permission collection are required");
        }
        var registeredIdentity = identities.registerIfAbsent(principal, clock.instant());
        var activeAssignments = assignments.findActiveAssignments(registeredIdentity.userId(), LocalDate.now(clock));
        Set<String> permissionValues = new TreeSet<>();
        externallyGrantedPermissions.stream()
                .map(ApplicationPermission::authority)
                .forEach(permissionValues::add);
        activeAssignments.stream()
                .map(RoleAssignment::profile)
                .flatMap(profile -> profile.permissions().stream())
                .map(ApplicationPermission::authority)
                .forEach(permissionValues::add);
        return new CurrentIdentitySnapshot(registeredIdentity.userId(), principal.subject(),
                permissionValues.stream().toList(), activeAssignments);
    }
}
