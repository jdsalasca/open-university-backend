package co.edu.uptc.universiry.identity.application;

import co.edu.uptc.universiry.identity.domain.AuthenticatedPrincipal;
import co.edu.uptc.universiry.identity.domain.RegisteredIdentity;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface IdentityDirectory {

    RegisteredIdentity registerIfAbsent(AuthenticatedPrincipal principal, Instant firstSeenAt);

    Optional<RegisteredIdentity> find(AuthenticatedPrincipal principal);

    List<RegisteredIdentity> findBySubjectPrefix(String subjectPrefix, int limit);
}
