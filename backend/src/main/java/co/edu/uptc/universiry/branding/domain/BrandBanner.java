package co.edu.uptc.universiry.branding.domain;

import java.time.Instant;

public record BrandBanner(
        String id,
        String assetId,
        String title,
        String altText,
        String placement,
        int displayOrder,
        Instant startsAt,
        Instant endsAt
) {
}
