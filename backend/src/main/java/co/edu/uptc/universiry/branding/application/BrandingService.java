package co.edu.uptc.universiry.branding.application;

import co.edu.uptc.universiry.branding.domain.BrandBanner;
import co.edu.uptc.universiry.branding.domain.BrandColor;
import co.edu.uptc.universiry.branding.domain.BrandModule;
import co.edu.uptc.universiry.branding.domain.BrandingConfiguration;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class BrandingService {

    private static final Set<String> COLOR_KEYS = Set.of("primary", "ink", "surface", "text", "accent", "focus");
    private static final Set<String> BANNER_PLACEMENTS = Set.of("home-hero", "login-banner", "announcement-strip");
    private static final Map<String, BrandModule> MODULE_CATALOG = BrandModule.defaultCatalog().stream()
            .collect(Collectors.toUnmodifiableMap(BrandModule::key, module -> module));

    private final BrandingRepository brandingRepository;
    private final BrandingContrastPolicy contrastPolicy;

    public BrandingService(BrandingRepository brandingRepository, BrandingContrastPolicy contrastPolicy) {
        this.brandingRepository = brandingRepository;
        this.contrastPolicy = contrastPolicy;
    }

    @Transactional(readOnly = true)
    public BrandingConfiguration currentAdministrativeConfiguration() {
        return brandingRepository.findCurrentAdministrative()
                .orElseThrow(() -> new IllegalStateException("The published branding configuration is missing."));
    }

    @Transactional
    public BrandingConfiguration publish(BrandingChange change, Actor actor, long expectedRevision) {
        if (expectedRevision < 1) {
            throw new BrandingValidationException("The expected revision must be positive.");
        }

        BrandingConfiguration current = brandingRepository.lockCurrentAdministrative()
                .orElseThrow(() -> new IllegalStateException("The published branding configuration is missing."));
        requireExpectedRevision(expectedRevision, current.revision());

        BrandingConfiguration next = buildConfiguration(change, current.revision() + 1);
        brandingRepository.insertRevision(next, actor, current.revision());
        brandingRepository.moveCurrentTo(next.revision());
        brandingRepository.appendAuditEvent(actor, "branding.publish", next.revision(), "{\"sourceRevision\":" + current.revision() + "}");
        return next;
    }

    @Transactional
    public BrandingConfiguration restore(long targetRevision, Actor actor, long expectedRevision) {
        if (targetRevision < 1 || expectedRevision < 1) {
            throw new BrandingValidationException("Revision values must be positive.");
        }

        BrandingConfiguration current = brandingRepository.lockCurrentAdministrative()
                .orElseThrow(() -> new IllegalStateException("The published branding configuration is missing."));
        requireExpectedRevision(expectedRevision, current.revision());
        BrandingConfiguration target = brandingRepository.findRevision(targetRevision)
                .orElseThrow(() -> new BrandingRevisionNotFoundException(targetRevision));
        if (targetRevision >= current.revision()) {
            throw new BrandingValidationException("A rollback target must be an earlier published revision.");
        }
        BrandingChange change = new BrandingChange(
                target.institutionName(),
                toHexMap(target.colors()),
                target.assets(),
                target.modules(),
                target.banners()
        );
        BrandingConfiguration next = buildConfiguration(change, current.revision() + 1);
        brandingRepository.insertRevision(next, actor, target.revision());
        brandingRepository.moveCurrentTo(next.revision());
        brandingRepository.appendAuditEvent(actor, "branding.rollback", next.revision(), "{\"sourceRevision\":" + target.revision() + "}");
        return next;
    }

    private BrandingConfiguration buildConfiguration(BrandingChange change, long revision) {
        if (change.institutionName() == null || change.institutionName().isBlank() || change.institutionName().strip().length() > 240) {
            throw new BrandingValidationException("The institution name must contain between 1 and 240 characters.");
        }
        if (!change.colors().keySet().equals(COLOR_KEYS)) {
            throw new BrandingValidationException("The color palette must contain exactly the six supported tokens.");
        }

        Map<String, BrandColor> colors = new LinkedHashMap<>();
        try {
            change.colors().forEach((key, value) -> colors.put(key, BrandColor.fromHex(value)));
        } catch (IllegalArgumentException exception) {
            throw new BrandingValidationException("Colors must use six-digit hexadecimal notation such as #FFCC29.");
        }
        contrastPolicy.validate(colors);

        BrandingConfiguration.Assets assets = validateAssets(change.assets());
        List<BrandModule> modules = validateModules(change.modules());
        List<BrandBanner> banners = validateBanners(change.banners());
        Set<String> referencedAssetIds = collectAssetIds(assets, banners);
        if (!brandingRepository.findExistingAssetIds(referencedAssetIds).containsAll(referencedAssetIds)) {
            throw new BrandingValidationException("A referenced image asset is not available for publication.");
        }

        return new BrandingConfiguration(
                revision,
                change.institutionName().strip(),
                colors,
                assets,
                modules,
                banners
        );
    }

    private BrandingConfiguration.Assets validateAssets(BrandingConfiguration.Assets assets) {
        return new BrandingConfiguration.Assets(
                validateAssetReference(assets.logoLight()),
                validateAssetReference(assets.logoDark()),
                validateAssetReference(assets.favicon())
        );
    }

    private String validateAssetReference(String assetId) {
        if (assetId == null) {
            return null;
        }
        if (!isCanonicalUuid(assetId)) {
            throw new BrandingValidationException("A referenced image asset has an invalid identifier.");
        }
        return assetId;
    }

    private Set<String> collectAssetIds(BrandingConfiguration.Assets assets, List<BrandBanner> banners) {
        Set<String> assetIds = new HashSet<>();
        if (assets.logoLight() != null) {
            assetIds.add(assets.logoLight());
        }
        if (assets.logoDark() != null) {
            assetIds.add(assets.logoDark());
        }
        if (assets.favicon() != null) {
            assetIds.add(assets.favicon());
        }
        banners.stream().map(BrandBanner::assetId).forEach(assetIds::add);
        return Set.copyOf(assetIds);
    }

    private List<BrandModule> validateModules(List<BrandModule> requestedModules) {
        if (requestedModules.size() != MODULE_CATALOG.size()) {
            throw new BrandingValidationException("All supported modules must remain in the configuration.");
        }

        Set<String> keys = new HashSet<>();
        Set<Integer> displayOrders = new HashSet<>();
        List<BrandModule> modules = requestedModules.stream().map(requested -> {
            BrandModule supported = MODULE_CATALOG.get(requested.key());
            if (supported == null || !keys.add(requested.key())) {
                throw new BrandingValidationException("The module catalog contains an unknown or duplicate key.");
            }
            String label = requested.label() == null ? "" : requested.label().strip();
            if (label.isEmpty() || label.length() > 100 || requested.displayOrder() < 0) {
                throw new BrandingValidationException("Module labels and order values are invalid.");
            }
            if (requested.visible() && !supported.available()) {
                throw new BrandingValidationException("A module cannot be shown before its functionality is available.");
            }
            if (!displayOrders.add(requested.displayOrder())) {
                throw new BrandingValidationException("Module display order values must be unique.");
            }
            return new BrandModule(supported.key(), label, supported.available(), requested.visible(), requested.displayOrder());
        }).toList();

        if (!keys.equals(MODULE_CATALOG.keySet())) {
            throw new BrandingValidationException("The module catalog must retain every stable module key.");
        }
        BrandModule visualIdentity = modules.stream().filter(module -> module.key().equals("visual-identity")).findFirst().orElseThrow();
        if (!visualIdentity.visible() || !visualIdentity.available()) {
            throw new BrandingValidationException("The visual identity center must remain available and visible.");
        }
        return modules;
    }

    private List<BrandBanner> validateBanners(List<BrandBanner> requestedBanners) {
        if (requestedBanners.size() > 12) {
            throw new BrandingValidationException("A configuration can contain at most twelve banners.");
        }

        Set<String> identifiers = new HashSet<>();
        return requestedBanners.stream().map(banner -> {
            String title = banner.title() == null ? "" : banner.title().strip();
            String altText = banner.altText() == null ? "" : banner.altText().strip();
            if (title.isEmpty() || title.length() > 160 || altText.isEmpty() || altText.length() > 300) {
                throw new BrandingValidationException("Each banner requires a title and meaningful alternative text.");
            }
            if (!BANNER_PLACEMENTS.contains(banner.placement()) || banner.displayOrder() < 0) {
                throw new BrandingValidationException("The banner placement or display order is invalid.");
            }
            if (banner.startsAt() != null && banner.endsAt() != null && !banner.endsAt().isAfter(banner.startsAt())) {
                throw new BrandingValidationException("A banner end time must be later than its start time.");
            }
            String id = banner.id() == null || banner.id().isBlank() ? UUID.randomUUID().toString() : banner.id();
            if (!isCanonicalUuid(id) || !identifiers.add(id)) {
                throw new BrandingValidationException("Banner identifiers must be unique generated UUIDs.");
            }
            String assetId = validateAssetReference(banner.assetId());
            if (assetId == null) {
                throw new BrandingValidationException("A banner image is required.");
            }
            return new BrandBanner(id, assetId, title, altText, banner.placement(), banner.displayOrder(), banner.startsAt(), banner.endsAt());
        }).toList();
    }

    private Map<String, String> toHexMap(Map<String, BrandColor> colors) {
        Map<String, String> result = new LinkedHashMap<>();
        colors.forEach((key, color) -> result.put(key, color.hex()));
        return result;
    }

    private void requireExpectedRevision(long expectedRevision, long currentRevision) {
        if (expectedRevision != currentRevision) {
            throw new BrandingRevisionConflictException();
        }
    }

    private boolean isCanonicalUuid(String value) {
        try {
            return UUID.fromString(value).toString().equalsIgnoreCase(value);
        } catch (IllegalArgumentException exception) {
            return false;
        }
    }
}
