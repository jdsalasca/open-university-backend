package co.edu.uptc.universiry.branding.infrastructure.web;

import co.edu.uptc.universiry.branding.domain.BrandBanner;
import co.edu.uptc.universiry.branding.domain.BrandColor;
import co.edu.uptc.universiry.branding.domain.BrandModule;
import co.edu.uptc.universiry.branding.domain.BrandingConfiguration;

import java.time.Instant;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public record BrandingResponse(
        long revision,
        String institutionName,
        Map<String, String> colors,
        AssetsResponse assets,
        List<ModuleResponse> modules,
        List<BannerResponse> banners
) {

    public BrandingResponse {
        colors = Collections.unmodifiableMap(new LinkedHashMap<>(colors));
        modules = List.copyOf(modules);
        banners = List.copyOf(banners);
    }

    public static BrandingResponse from(BrandingConfiguration configuration) {
        Map<String, String> colors = new LinkedHashMap<>();
        configuration.colors().forEach((key, color) -> colors.put(key, color.hex()));

        return new BrandingResponse(
                configuration.revision(),
                configuration.institutionName(),
                colors,
                new AssetsResponse(
                        configuration.assets().logoLight(),
                        configuration.assets().logoDark(),
                        configuration.assets().favicon()
                ),
                configuration.modules().stream().map(ModuleResponse::from).toList(),
                configuration.banners().stream().map(BannerResponse::from).toList()
        );
    }

    public record AssetsResponse(String logoLight, String logoDark, String favicon) {
    }

    public record ModuleResponse(String key, String label, boolean available, boolean visible, int order) {

        private static ModuleResponse from(BrandModule module) {
            return new ModuleResponse(module.key(), module.label(), module.available(), module.visible(), module.displayOrder());
        }
    }

    public record BannerResponse(
            String id,
            String assetId,
            String title,
            String altText,
            String placement,
            int order,
            Instant startsAt,
            Instant endsAt
    ) {

        private static BannerResponse from(BrandBanner banner) {
            return new BannerResponse(
                    banner.id(),
                    banner.assetId(),
                    banner.title(),
                    banner.altText(),
                    banner.placement(),
                    banner.displayOrder(),
                    banner.startsAt(),
                    banner.endsAt()
            );
        }
    }
}
