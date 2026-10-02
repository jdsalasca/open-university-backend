package co.edu.uptc.universiry.admissions.application;

import co.edu.uptc.universiry.admissions.domain.AdmissionsActor;
import co.edu.uptc.universiry.identity.domain.AuthenticatedPrincipal;

public interface AdmissionsActorResolver {
    AdmissionsActor resolve(AuthenticatedPrincipal principal);
}
