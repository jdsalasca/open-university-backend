package co.edu.uptc.universiry.branding.infrastructure.persistence;

import co.edu.uptc.universiry.branding.application.BrandingRepository;
import co.edu.uptc.universiry.branding.domain.BrandingConfiguration;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class BrandingRepositoryIntegrationTest {

    @Autowired
    private BrandingRepository brandingRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void reads_the_persisted_current_configuration() {
        // Arrange
        Instant requestTime = Instant.parse("2026-09-29T12:00:00Z");

        // Act
        BrandingConfiguration configuration = brandingRepository.findCurrentPublic(requestTime).orElseThrow();

        // Assert
        assertEquals(1, configuration.revision());
        assertEquals("#FFCC29", configuration.colors().get("primary").hex());
        assertEquals("#1A1A1A", configuration.colors().get("ink").hex());
        assertEquals(List.of(
                "home", "students", "programs", "curricula", "subjects", "academic-load", "spaces", "visual-identity", "admissions"
        ), configuration.modules().stream().map(module -> module.key()).toList());

        Integer revisions = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM institution_branding_revision", Integer.class);
        Integer spaceLabels = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM institution_module_label WHERE module_key = 'spaces'", Integer.class);
        assertEquals(revisions, spaceLabels);
    }

    @Test
    void public_configuration_includes_only_banners_active_at_request_time() {
        // Arrange
        String assetId = UUID.randomUUID().toString();
        jdbcTemplate.update(
                "INSERT INTO media_asset (asset_id, storage_key, mime_type, size_bytes, width_px, height_px, sha256, uploaded_by, uploaded_at) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)",
                assetId, "test-asset", "image/png", 32, 1, 1, "0".repeat(64), "test-operator", LocalDateTime.of(2026, 9, 29, 11, 0)
        );
        insertBanner(assetId, "active", LocalDateTime.of(2026, 9, 29, 11, 59), LocalDateTime.of(2026, 9, 29, 12, 1), 10);
        insertBanner(assetId, "expired", LocalDateTime.of(2026, 9, 29, 10, 0), LocalDateTime.of(2026, 9, 29, 11, 59), 20);
        insertBanner(assetId, "scheduled", LocalDateTime.of(2026, 9, 29, 12, 1), LocalDateTime.of(2026, 9, 29, 13, 0), 30);

        // Act
        BrandingConfiguration configuration = brandingRepository.findCurrentPublic(Instant.parse("2026-09-29T12:00:00Z")).orElseThrow();

        // Assert
        assertEquals(List.of("active"), configuration.banners().stream().map(banner -> banner.title()).toList());
    }

    private void insertBanner(String assetId, String title, LocalDateTime startsAt, LocalDateTime endsAt, int order) {
        jdbcTemplate.update(
                "INSERT INTO institution_banner (revision_id, banner_key, asset_id, title, alt_text, placement, display_order, starts_at, ends_at) VALUES (1, ?, ?, ?, ?, ?, ?, ?, ?)",
                UUID.randomUUID().toString(), assetId, title, "Descripción accesible de " + title, "home-hero", order, startsAt, endsAt
        );
    }
}
