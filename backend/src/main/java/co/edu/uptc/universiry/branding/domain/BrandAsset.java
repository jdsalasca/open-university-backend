package co.edu.uptc.universiry.branding.domain;

import java.util.Set;
import java.util.UUID;

public record BrandAsset(
        String assetId,
        String storageKey,
        String mimeType,
        long sizeBytes,
        int widthPx,
        int heightPx,
        String sha256
) {

    private static final Set<String> ALLOWED_MIME_TYPES = Set.of("image/png", "image/jpeg", "image/webp");

    public BrandAsset {
        if (assetId == null || !isCanonicalUuid(assetId)) {
            throw new IllegalArgumentException("An asset requires a canonical UUID identifier.");
        }
        if (storageKey == null || !storageKey.matches("[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}\\.asset")) {
            throw new IllegalArgumentException("An asset requires a generated storage key.");
        }
        if (!ALLOWED_MIME_TYPES.contains(mimeType) || sizeBytes <= 0 || widthPx <= 0 || heightPx <= 0) {
            throw new IllegalArgumentException("The asset metadata is invalid.");
        }
        if (sha256 == null || !sha256.matches("[0-9a-f]{64}")) {
            throw new IllegalArgumentException("An asset requires a SHA-256 checksum.");
        }
    }

    private static boolean isCanonicalUuid(String value) {
        try {
            return UUID.fromString(value).toString().equals(value);
        } catch (IllegalArgumentException exception) {
            return false;
        }
    }
}
