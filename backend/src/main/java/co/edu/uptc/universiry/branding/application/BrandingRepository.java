package co.edu.uptc.universiry.branding.application;

import co.edu.uptc.universiry.branding.domain.BrandingConfiguration;

import java.time.Instant;
import java.util.Optional;
import java.util.Set;

public interface BrandingRepository {

    Optional<BrandingConfiguration> findCurrentPublic(Instant requestTime);

    Optional<BrandingConfiguration> findCurrentAdministrative();

    Optional<BrandingConfiguration> lockCurrentAdministrative();

    Optional<BrandingConfiguration> findRevision(long revision);

    void insertRevision(BrandingConfiguration configuration, Actor actor, Long sourceRevision);

    void moveCurrentTo(long revision);

    void appendAuditEvent(Actor actor, String action, long revision, String changeSummary);

    long countAuditEvents(long revision);

    Set<String> findExistingAssetIds(Set<String> assetIds);
}
