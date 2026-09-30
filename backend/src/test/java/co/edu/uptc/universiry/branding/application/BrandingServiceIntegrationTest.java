package co.edu.uptc.universiry.branding.application;

import co.edu.uptc.universiry.branding.domain.BrandModule;
import co.edu.uptc.universiry.branding.domain.BrandingConfiguration;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class BrandingServiceIntegrationTest {

    private static final Actor ADMIN = new Actor("synthetic-brand-admin");

    @Autowired
    private BrandingService brandingService;

    @Autowired
    private BrandingRepository brandingRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void publishes_a_new_immutable_revision_and_audit_event() {
        // Arrange
        BrandingChange change = change("Institución UPTC", "#E0C037", "#1A1A1A", "Portal estudiantil");

        // Act
        BrandingConfiguration published = brandingService.publish(change, ADMIN, 1);

        // Assert
        assertEquals(2, published.revision());
        assertEquals("#E0C037", published.colors().get("primary").hex());
        assertEquals("#FFCC29", brandingRepository.findRevision(1).orElseThrow().colors().get("primary").hex());
        assertEquals(1, brandingRepository.countAuditEvents(2));
        assertEquals("synthetic-brand-admin", jdbcTemplate.queryForObject(
                "SELECT actor_sub FROM administrative_audit_event WHERE revision_id = 2", String.class
        ));
    }

    @Test
    void rejects_stale_revision_without_overwriting_the_current_snapshot() {
        // Arrange
        brandingService.publish(change("Primera publicación", "#E0C037", "#1A1A1A", "Estudiantes"), ADMIN, 1);
        BrandingChange staleChange = change("Cambio obsoleto", "#E0C037", "#1A1A1A", "Estudiantes");

        // Act + Assert
        assertThrows(BrandingRevisionConflictException.class, () -> brandingService.publish(staleChange, ADMIN, 1));
        assertEquals("Primera publicación", brandingRepository.findCurrentAdministrative().orElseThrow().institutionName());
    }

    @Test
    void rejects_unknown_module_key_without_publishing() {
        // Arrange
        BrandingChange invalidChange = changeWithUnknownModuleKey();

        // Act + Assert
        assertThrows(BrandingValidationException.class, () -> brandingService.publish(invalidChange, ADMIN, 1));
        assertEquals(1, brandingRepository.findCurrentAdministrative().orElseThrow().revision());
    }

    @Test
    void does_not_allow_unavailable_modules_to_appear_in_navigation() {
        // Arrange
        BrandingChange validChange = change("UPTC", "#E0C037", "#1A1A1A", "Estudiantes");
        List<BrandModule> modules = validChange.modules().stream()
                .map(module -> module.key().equals("programs")
                        ? new BrandModule(module.key(), module.label(), module.available(), true, module.displayOrder())
                        : module)
                .toList();
        BrandingChange invalidChange = new BrandingChange(
                validChange.institutionName(), validChange.colors(), validChange.assets(), modules, validChange.banners()
        );

        // Act + Assert
        assertThrows(BrandingValidationException.class, () -> brandingService.publish(invalidChange, ADMIN, 1));
        assertEquals(1, brandingRepository.findCurrentAdministrative().orElseThrow().revision());
    }

    @Test
    void visual_identity_center_cannot_be_hidden() {
        // Arrange
        BrandingChange validChange = change("UPTC", "#E0C037", "#1A1A1A", "Estudiantes");
        List<BrandModule> modules = validChange.modules().stream()
                .map(module -> module.key().equals("visual-identity")
                        ? new BrandModule(module.key(), module.label(), module.available(), false, module.displayOrder())
                        : module)
                .toList();
        BrandingChange invalidChange = new BrandingChange(
                validChange.institutionName(), validChange.colors(), validChange.assets(), modules, validChange.banners()
        );

        // Act + Assert
        assertThrows(BrandingValidationException.class, () -> brandingService.publish(invalidChange, ADMIN, 1));
        assertEquals(1, brandingRepository.findCurrentAdministrative().orElseThrow().revision());
    }

    @Test
    void rejects_invalid_hex_and_blank_module_label() {
        // Arrange
        BrandingChange invalidColor = change("UPTC", "url(javascript:alert(1))", "#1A1A1A", "Estudiantes");
        BrandingChange blankLabel = change("UPTC", "#E0C037", "#1A1A1A", "   ");

        // Act + Assert
        assertThrows(BrandingValidationException.class, () -> brandingService.publish(invalidColor, ADMIN, 1));
        assertThrows(BrandingValidationException.class, () -> brandingService.publish(blankLabel, ADMIN, 1));
        assertEquals(1, brandingRepository.findCurrentAdministrative().orElseThrow().revision());
    }

    @Test
    void rejects_unregistered_asset_references_before_publication() {
        // Arrange
        BrandingChange validChange = change("UPTC", "#E0C037", "#1A1A1A", "Estudiantes");
        BrandingChange withUnknownAsset = new BrandingChange(
                validChange.institutionName(),
                validChange.colors(),
                new BrandingConfiguration.Assets(UUID.randomUUID().toString(), null, null),
                validChange.modules(),
                List.of()
        );

        // Act + Assert
        assertThrows(BrandingValidationException.class, () -> brandingService.publish(withUnknownAsset, ADMIN, 1));
        assertEquals(1, brandingRepository.findCurrentAdministrative().orElseThrow().revision());
        assertEquals(0, brandingRepository.countAuditEvents(2));
    }

    @Test
    void blocks_text_contrast_below_4_5_and_accepts_4_5() {
        // Arrange
        BrandingChange belowThreshold = change("UPTC", "#FFCC29", "#597F6C", "Estudiantes");
        BrandingChange atThreshold = change("UPTC", "#FFCC29", "#7C7290", "Estudiantes");

        // Act + Assert
        assertThrows(BrandingValidationException.class, () -> brandingService.publish(belowThreshold, ADMIN, 1));
        assertEquals(2, brandingService.publish(atThreshold, ADMIN, 1).revision());
    }

    @Test
    void restores_a_historical_snapshot_into_a_new_revision() {
        // Arrange
        for (long expectedRevision = 1; expectedRevision < 8; expectedRevision++) {
            brandingService.publish(
                    change("UPTC revisión " + (expectedRevision + 1), "#FFCC29", "#1A1A1A", "Estudiantes"),
                    ADMIN,
                    expectedRevision
            );
        }
        String revisionThreeName = brandingRepository.findRevision(3).orElseThrow().institutionName();
        String revisionEightName = brandingRepository.findRevision(8).orElseThrow().institutionName();

        // Act
        BrandingConfiguration restored = brandingService.restore(3, ADMIN, 8);

        // Assert
        assertEquals(9, restored.revision());
        assertEquals(revisionThreeName, restored.institutionName());
        assertEquals(revisionThreeName, brandingRepository.findRevision(3).orElseThrow().institutionName());
        assertEquals(revisionEightName, brandingRepository.findRevision(8).orElseThrow().institutionName());
    }

    @Test
    void rejects_missing_and_current_revision_as_rollback_targets() {
        // Arrange
        brandingService.publish(change("UPTC revisión 2", "#E0C037", "#1A1A1A", "Estudiantes"), ADMIN, 1);

        // Act + Assert
        assertThrows(BrandingRevisionNotFoundException.class, () -> brandingService.restore(99, ADMIN, 2));
        assertThrows(BrandingValidationException.class, () -> brandingService.restore(2, ADMIN, 2));
        assertEquals(2, brandingRepository.findCurrentAdministrative().orElseThrow().revision());
    }

    private static BrandingChange change(String institutionName, String primaryColor, String textColor, String studentLabel) {
        BrandingConfiguration defaults = BrandingConfiguration.defaults();
        Map<String, String> colors = new LinkedHashMap<>();
        defaults.colors().forEach((key, color) -> colors.put(key, color.hex()));
        colors.put("primary", primaryColor);
        colors.put("text", textColor);

        List<BrandModule> modules = defaults.modules().stream()
                .map(module -> module.key().equals("students")
                        ? new BrandModule(module.key(), studentLabel, module.available(), module.visible(), module.displayOrder())
                        : module)
                .toList();

        return new BrandingChange(
                institutionName,
                colors,
                new BrandingConfiguration.Assets(null, null, null),
                modules,
                List.of()
        );
    }

    private static BrandingChange changeWithUnknownModuleKey() {
        BrandingChange validChange = change("UPTC", "#E0C037", "#1A1A1A", "Estudiantes");
        List<BrandModule> modules = validChange.modules().stream()
                .map(module -> module.key().equals("students")
                        ? new BrandModule("unknown-module", module.label(), module.available(), module.visible(), module.displayOrder())
                        : module)
                .toList();
        return new BrandingChange(
                validChange.institutionName(),
                validChange.colors(),
                validChange.assets(),
                modules,
                validChange.banners()
        );
    }
}
