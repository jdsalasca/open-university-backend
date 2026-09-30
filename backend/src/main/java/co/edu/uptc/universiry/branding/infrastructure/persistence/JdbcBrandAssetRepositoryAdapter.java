package co.edu.uptc.universiry.branding.infrastructure.persistence;

import co.edu.uptc.universiry.branding.application.Actor;
import co.edu.uptc.universiry.branding.application.BrandAssetRepository;
import co.edu.uptc.universiry.branding.domain.BrandAsset;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;

@Repository
public class JdbcBrandAssetRepositoryAdapter implements BrandAssetRepository {

    private final JdbcTemplate jdbcTemplate;

    public JdbcBrandAssetRepositoryAdapter(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public void insert(BrandAsset asset, Actor actor) {
        LocalDateTime now = LocalDateTime.now(ZoneOffset.UTC);
        jdbcTemplate.update(
                """
                        INSERT INTO media_asset (
                            asset_id, storage_key, mime_type, size_bytes, width_px, height_px,
                            sha256, uploaded_by, uploaded_at
                        ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
                        """,
                asset.assetId(), asset.storageKey(), asset.mimeType(), asset.sizeBytes(), asset.widthPx(),
                asset.heightPx(), asset.sha256(), actor.subject(), now
        );

        Long currentRevision = jdbcTemplate.queryForObject(
                "SELECT revision_id FROM institution_branding_current WHERE singleton_id = 1", Long.class
        );
        if (currentRevision == null) {
            throw new IllegalStateException("The current branding revision is missing.");
        }
        jdbcTemplate.update(
                """
                        INSERT INTO administrative_audit_event (
                            actor_sub, action_key, entity_type, revision_id, occurred_at, change_summary
                        ) VALUES (?, 'branding.asset.upload', 'media-asset', ?, ?, ?)
                        """,
                actor.subject(), currentRevision, now, "{\"assetId\":\"" + asset.assetId() + "\"}"
        );
    }

    @Override
    public Optional<BrandAsset> findCurrentlyPublished(String assetId, Instant requestTime) {
        LocalDateTime now = LocalDateTime.ofInstant(requestTime, ZoneOffset.UTC);
        List<BrandAsset> assets = jdbcTemplate.query(
                """
                        SELECT media.asset_id, media.storage_key, media.mime_type, media.size_bytes,
                               media.width_px, media.height_px, media.sha256
                        FROM media_asset media
                        WHERE media.asset_id = ?
                          AND (
                            EXISTS (
                                SELECT 1
                                FROM institution_branding_current bc
                                JOIN institution_branding_revision br
                                  ON br.revision_id = bc.revision_id
                                WHERE bc.singleton_id = 1
                                  AND (br.logo_light_asset_id = media.asset_id
                                       OR br.logo_dark_asset_id = media.asset_id
                                       OR br.favicon_asset_id = media.asset_id)
                            )
                            OR EXISTS (
                                SELECT 1
                                FROM institution_branding_current bc
                                JOIN institution_banner banner
                                  ON banner.revision_id = bc.revision_id
                                WHERE bc.singleton_id = 1
                                  AND banner.asset_id = media.asset_id
                                  AND (banner.starts_at IS NULL OR banner.starts_at <= ?)
                                  AND (banner.ends_at IS NULL OR banner.ends_at > ?)
                            )
                          )
                        """,
                (resultSet, rowNumber) -> new BrandAsset(
                        resultSet.getString("asset_id"),
                        resultSet.getString("storage_key"),
                        resultSet.getString("mime_type"),
                        resultSet.getLong("size_bytes"),
                        resultSet.getInt("width_px"),
                        resultSet.getInt("height_px"),
                        resultSet.getString("sha256")
                ),
                assetId, now, now
        );
        return assets.stream().findFirst();
    }
}
