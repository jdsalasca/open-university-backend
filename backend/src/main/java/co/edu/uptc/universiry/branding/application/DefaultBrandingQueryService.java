package co.edu.uptc.universiry.branding.application;

import co.edu.uptc.universiry.branding.domain.BrandingConfiguration;
import org.springframework.stereotype.Service;

import java.time.Clock;

@Service
public class DefaultBrandingQueryService implements BrandingQueryService {

    private final BrandingRepository brandingRepository;
    private final Clock clock;

    public DefaultBrandingQueryService(BrandingRepository brandingRepository, Clock clock) {
        this.brandingRepository = brandingRepository;
        this.clock = clock;
    }

    @Override
    public BrandingConfiguration currentPublicConfiguration() {
        return brandingRepository.findCurrentPublic(clock.instant())
                .orElseThrow(() -> new IllegalStateException("The published branding configuration is missing."));
    }
}
