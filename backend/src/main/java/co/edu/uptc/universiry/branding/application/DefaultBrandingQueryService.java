package co.edu.uptc.universiry.branding.application;

import co.edu.uptc.universiry.branding.domain.BrandingConfiguration;
import org.springframework.stereotype.Service;

@Service
public class DefaultBrandingQueryService implements BrandingQueryService {

    @Override
    public BrandingConfiguration currentPublicConfiguration() {
        return BrandingConfiguration.defaults();
    }
}
