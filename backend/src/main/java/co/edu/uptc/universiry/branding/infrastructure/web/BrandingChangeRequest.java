package co.edu.uptc.universiry.branding.infrastructure.web;

import co.edu.uptc.universiry.branding.application.BrandingChange;
import co.edu.uptc.universiry.branding.domain.BrandBanner;
import co.edu.uptc.universiry.branding.domain.BrandModule;
import co.edu.uptc.universiry.branding.domain.BrandingConfiguration;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

import java.time.Instant;
import java.util.List;
import java.util.Map;

public record BrandingChangeRequest(
        @NotNull @Positive Long expectedRevision,
        @NotBlank @Size(max = 240) String institutionName,
        @NotNull @Size(min = 6, max = 6)
        Map<@NotBlank @Size(max = 32) String, @NotBlank @Pattern(regexp = "^#[0-9A-Fa-f]{6}$") String> colors,
        @NotNull @Valid AssetsRequest assets,
        @NotNull @Size(min = 7, max = 7) List<@NotNull @Valid ModuleRequest> modules,
        @NotNull @Size(max = 12) List<@NotNull @Valid BannerRequest> banners
) {

    public BrandingChange toChange() {
        return new BrandingChange(
                institutionName,
                colors,
                new BrandingConfiguration.Assets(assets.logoLight(), assets.logoDark(), assets.favicon()),
                modules.stream().map(ModuleRequest::toDomain).toList(),
                banners.stream().map(BannerRequest::toDomain).toList()
        );
    }

    public record AssetsRequest(
            @Pattern(regexp = "^$|^[0-9a-fA-F-]{36}$") String logoLight,
            @Pattern(regexp = "^$|^[0-9a-fA-F-]{36}$") String logoDark,
            @Pattern(regexp = "^$|^[0-9a-fA-F-]{36}$") String favicon
    ) {
    }

    public record ModuleRequest(
            @NotBlank @Size(max = 64) String key,
            @NotBlank @Size(max = 100) String label,
            boolean available,
            boolean visible,
            @NotNull @PositiveOrZero Integer order
    ) {

        private BrandModule toDomain() {
            return new BrandModule(key, label, available, visible, order);
        }
    }

    public record BannerRequest(
            @Pattern(regexp = "^$|^[0-9a-fA-F-]{36}$") String id,
            @NotBlank @Pattern(regexp = "^[0-9a-fA-F-]{36}$") String assetId,
            @NotBlank @Size(max = 160) String title,
            @NotBlank @Size(max = 300) String altText,
            @NotBlank @Size(max = 64) String placement,
            @NotNull @PositiveOrZero Integer order,
            Instant startsAt,
            Instant endsAt
    ) {

        private BrandBanner toDomain() {
            return new BrandBanner(id, assetId, title, altText, placement, order, startsAt, endsAt);
        }
    }
}
