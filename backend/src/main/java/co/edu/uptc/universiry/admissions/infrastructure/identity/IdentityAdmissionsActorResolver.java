package co.edu.uptc.universiry.admissions.infrastructure.identity;

import co.edu.uptc.universiry.admissions.application.AdmissionsActorException;
import co.edu.uptc.universiry.admissions.application.AdmissionsActorResolver;
import co.edu.uptc.universiry.admissions.domain.AdmissionsActor;
import co.edu.uptc.universiry.identity.application.IdentityDirectory;
import co.edu.uptc.universiry.identity.domain.AuthenticatedPrincipal;
import org.springframework.stereotype.Component;

@Component
public class IdentityAdmissionsActorResolver implements AdmissionsActorResolver {

    private final IdentityDirectory identities;

    public IdentityAdmissionsActorResolver(IdentityDirectory identities) {
        this.identities = identities;
    }

    @Override
    public AdmissionsActor resolve(AuthenticatedPrincipal principal) {
        if (principal == null) throw new AdmissionsActorException();
        return identities.find(principal)
                .map(identity -> new AdmissionsActor(identity.userId(), identity.id()))
                .orElseThrow(AdmissionsActorException::new);
    }
}
