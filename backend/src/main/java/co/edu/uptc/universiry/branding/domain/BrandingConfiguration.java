package co.edu.uptc.universiry.branding.domain;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public record BrandingConfiguration(
        long revision,
        String institutionName,
        Map<String, BrandColor> colors,
        Assets assets,
        List<BrandModule> modules,
        List<BrandBanner> banners
) {

    public BrandingConfiguration {
        if (revision < 1) {
            throw new IllegalArgumentException("A branding revision must be positive.");
        }
        if (institutionName == null || institutionName.isBlank()) {
            throw new IllegalArgumentException("The institution name is required.");
        }

        colors = Collections.unmodifiableMap(new LinkedHashMap<>(Objects.requireNonNull(colors)));
        assets = Objects.requireNonNull(assets);
        modules = List.copyOf(Objects.requireNonNull(modules));
        banners = List.copyOf(Objects.requireNonNull(banners));
    }

    public static BrandingConfiguration defaults() {
        Map<String, BrandColor> colors = new LinkedHashMap<>();
        colors.put("primary", BrandColor.fromHex("#FFCC29"));
        colors.put("ink", BrandColor.fromHex("#1A1A1A"));
        colors.put("surface", BrandColor.fromHex("#FFFFFF"));
        colors.put("text", BrandColor.fromHex("#1A1A1A"));
        colors.put("accent", BrandColor.fromHex("#FFCC29"));
        colors.put("focus", BrandColor.fromHex("#1A1A1A"));

        return new BrandingConfiguration(
                1,
                "Universidad Pedagógica y Tecnológica de Colombia",
                colors,
                new Assets(null, null, null),
                BrandModule.defaultCatalog(),
                List.of()
        );
    }

    public record Assets(String logoLight, String logoDark, String favicon) {
    }
}
