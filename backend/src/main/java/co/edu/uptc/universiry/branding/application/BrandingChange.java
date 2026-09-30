package co.edu.uptc.universiry.branding.application;

import co.edu.uptc.universiry.branding.domain.BrandBanner;
import co.edu.uptc.universiry.branding.domain.BrandModule;
import co.edu.uptc.universiry.branding.domain.BrandingConfiguration;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public record BrandingChange(
        String institutionName,
        Map<String, String> colors,
        BrandingConfiguration.Assets assets,
        List<BrandModule> modules,
        List<BrandBanner> banners
) {

    public BrandingChange {
        colors = Collections.unmodifiableMap(new LinkedHashMap<>(Objects.requireNonNull(colors)));
        assets = Objects.requireNonNull(assets);
        modules = List.copyOf(Objects.requireNonNull(modules));
        banners = List.copyOf(Objects.requireNonNull(banners));
    }
}
