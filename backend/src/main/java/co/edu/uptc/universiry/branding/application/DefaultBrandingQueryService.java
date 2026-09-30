package co.edu.uptc.universiry.branding.application;

import co.edu.uptc.universiry.branding.domain.BrandingConfiguration;
import org.springframework.stereotype.Service;

import java.time.Instant;

@Service
public class DefaultBrandingQueryService implements BrandingQueryService {

    private final BrandingRepository brandingRepository;

    public DefaultBrandingQueryService(BrandingRepository brandingRepository) {
        this.brandingRepository = brandingRepository;
    }

    @Override
    public BrandingConfiguration currentPublicConfiguration() {
        return brandingRepository.findCurrentPublic(Instant.now())
                .orElseThrow(() -> new IllegalStateException("The published branding configuration is missing."));
    }
}
