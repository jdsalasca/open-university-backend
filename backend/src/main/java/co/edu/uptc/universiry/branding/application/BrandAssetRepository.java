package co.edu.uptc.universiry.branding.application;

import co.edu.uptc.universiry.branding.domain.BrandAsset;

import java.time.Instant;
import java.util.Optional;

public interface BrandAssetRepository {

    void insert(BrandAsset asset, Actor actor);

    Optional<BrandAsset> findCurrentlyPublished(String assetId, Instant requestTime);
}
