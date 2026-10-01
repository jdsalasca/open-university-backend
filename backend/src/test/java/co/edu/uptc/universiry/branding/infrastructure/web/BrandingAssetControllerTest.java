package co.edu.uptc.universiry.branding.infrastructure.web;

import co.edu.uptc.universiry.security.WithBrandingPermissions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.HexFormat;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = {
        "branding.assets.max-bytes=2048",
        "branding.assets.max-width=64",
        "branding.assets.max-height=64",
        "branding.assets.max-pixels=2048",
        "spring.servlet.multipart.max-file-size=2KB",
        "spring.servlet.multipart.max-request-size=4KB"
})
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class BrandingAssetControllerTest {

    @TempDir
    static Path assetDirectory;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @DynamicPropertySource
    static void assetStorageDirectory(DynamicPropertyRegistry registry) {
        registry.add("branding.assets.directory", () -> assetDirectory.toString());
    }

    @BeforeEach
    void ensureAssetDirectoryExists() throws IOException {
        Files.createDirectories(assetDirectory);
    }

    @Test
    @WithBrandingPermissions
    void brand_admin_uploads_png_and_receives_opaque_metadata() throws Exception {
        // Arrange
        byte[] png = pngBytes(32, 24);
        MockMultipartFile upload = new MockMultipartFile("file", "../../institution-logo.png", MediaType.IMAGE_PNG_VALUE, png);

        // Act
        String responseBody = mockMvc.perform(multipart("/api/v1/admin/branding/assets").file(upload))
                // Assert
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.mimeType").value(MediaType.IMAGE_PNG_VALUE))
                .andExpect(jsonPath("$.sizeBytes").value(png.length))
                .andExpect(jsonPath("$.width").value(32))
                .andExpect(jsonPath("$.height").value(24))
                .andExpect(jsonPath("$.storageKey").doesNotExist())
                .andExpect(jsonPath("$.originalFilename").doesNotExist())
                .andReturn().getResponse().getContentAsString();

        // Assert
        String assetId = assetIdFrom(responseBody);
        assertEquals(UUID.fromString(assetId).toString(), assetId);
        assertEquals(1, jdbcTemplate.queryForObject("SELECT COUNT(*) FROM media_asset WHERE asset_id = ?", Integer.class, assetId));
        assertEquals(1, jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM administrative_audit_event WHERE actor_sub = ? AND action_key = ? AND entity_type = ? AND revision_id = 1",
                Integer.class, "brand.editor", "branding.asset.upload", "media-asset"
        ));
        assertEquals(1, fileCount());
        try (var files = Files.list(assetDirectory)) {
            assertTrue(files.findFirst().orElseThrow().getFileName().toString().matches("[0-9a-f-]{36}\\.asset"));
        }
    }

    @Test
    @WithBrandingPermissions
    void rejects_non_image_bytes_despite_png_filename_without_persisting_file_or_metadata() throws Exception {
        // Arrange
        MockMultipartFile upload = new MockMultipartFile("file", "logo.png", MediaType.IMAGE_PNG_VALUE,
                "this is not a PNG".getBytes(java.nio.charset.StandardCharsets.UTF_8));

        // Act + Assert
        mockMvc.perform(multipart("/api/v1/admin/branding/assets").file(upload))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("validation_failed"));
        assertEquals(0, jdbcTemplate.queryForObject("SELECT COUNT(*) FROM media_asset", Integer.class));
        assertEquals(0, jdbcTemplate.queryForObject("SELECT COUNT(*) FROM administrative_audit_event WHERE entity_type = 'media-asset'", Integer.class));
        assertEquals(0, fileCount());
        assertEquals(1, jdbcTemplate.queryForObject("SELECT revision_id FROM institution_branding_current WHERE singleton_id = 1", Integer.class));
    }

    @Test
    @WithBrandingPermissions
    void rejects_bytes_above_the_configured_upload_limit() throws Exception {
        // Arrange
        MockMultipartFile upload = new MockMultipartFile("file", "oversized.png", MediaType.IMAGE_PNG_VALUE, new byte[2049]);

        // Act + Assert
        mockMvc.perform(multipart("/api/v1/admin/branding/assets").file(upload))
                .andExpect(status().is(413));
        assertEquals(0, jdbcTemplate.queryForObject("SELECT COUNT(*) FROM media_asset", Integer.class));
        assertEquals(0, jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM administrative_audit_event WHERE entity_type = 'media-asset'",
                Integer.class
        ));
        assertEquals(0, fileCount());
    }

    @Test
    @WithBrandingPermissions
    void rejects_images_exceeding_dimension_limits_before_persisting() throws Exception {
        // Arrange
        MockMultipartFile upload = new MockMultipartFile("file", "too-wide.png", MediaType.IMAGE_PNG_VALUE, pngBytes(65, 16));

        // Act + Assert
        mockMvc.perform(multipart("/api/v1/admin/branding/assets").file(upload))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("validation_failed"));
        assertEquals(0, jdbcTemplate.queryForObject("SELECT COUNT(*) FROM media_asset", Integer.class));
        assertEquals(0, fileCount());
    }

    @Test
    @WithBrandingPermissions
    void accepts_a_decodable_webp_and_derives_its_mime_type_and_dimensions() throws Exception {
        // Arrange
        byte[] webp = webpBytes();
        MockMultipartFile upload = new MockMultipartFile("file", "banner.webp", "image/webp", webp);

        // Act + Assert
        mockMvc.perform(multipart("/api/v1/admin/branding/assets").file(upload))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.mimeType").value("image/webp"))
                .andExpect(jsonPath("$.sizeBytes").value(webp.length))
                .andExpect(jsonPath("$.width").value(1))
                .andExpect(jsonPath("$.height").value(1));
    }

    @Test
    @WithBrandingPermissions
    void rejects_a_webp_container_with_unaccounted_trailing_bytes() throws Exception {
        // Arrange
        byte[] validWebp = webpBytes();
        byte[] webpWithTrailingData = java.util.Arrays.copyOf(validWebp, validWebp.length + 1);
        webpWithTrailingData[webpWithTrailingData.length - 1] = 0x41;
        MockMultipartFile upload = new MockMultipartFile("file", "banner.webp", "image/webp", webpWithTrailingData);

        // Act + Assert
        mockMvc.perform(multipart("/api/v1/admin/branding/assets").file(upload))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("validation_failed"));
        assertEquals(0, jdbcTemplate.queryForObject("SELECT COUNT(*) FROM media_asset", Integer.class));
        assertEquals(0, fileCount());
    }

    @Test
    @WithBrandingPermissions
    void accepts_a_decodable_jpeg_and_uses_its_detected_mime_type() throws Exception {
        // Arrange
        byte[] jpeg = jpegBytes(20, 12);
        MockMultipartFile upload = new MockMultipartFile("file", "header.jpeg", "image/jpeg", jpeg);

        // Act + Assert
        mockMvc.perform(multipart("/api/v1/admin/branding/assets").file(upload))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.mimeType").value("image/jpeg"))
                .andExpect(jsonPath("$.width").value(20))
                .andExpect(jsonPath("$.height").value(12));
    }

    @Test
    @WithBrandingPermissions
    void rejects_svg_even_when_it_is_declared_as_an_image() throws Exception {
        // Arrange
        byte[] svg = "<svg xmlns=\"http://www.w3.org/2000/svg\"><script>alert(1)</script></svg>"
                .getBytes(java.nio.charset.StandardCharsets.UTF_8);
        MockMultipartFile upload = new MockMultipartFile("file", "logo.svg", "image/svg+xml", svg);

        // Act + Assert
        mockMvc.perform(multipart("/api/v1/admin/branding/assets").file(upload))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("validation_failed"));
        assertEquals(0, jdbcTemplate.queryForObject("SELECT COUNT(*) FROM media_asset", Integer.class));
        assertEquals(0, fileCount());
    }

    @Test
    @WithBrandingPermissions
    void rejects_images_that_exceed_the_pixel_budget_even_when_each_side_is_allowed() throws Exception {
        // Arrange
        MockMultipartFile upload = new MockMultipartFile("file", "too-many-pixels.png", MediaType.IMAGE_PNG_VALUE,
                pngBytes(48, 48));

        // Act + Assert
        mockMvc.perform(multipart("/api/v1/admin/branding/assets").file(upload))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("validation_failed"));
        assertEquals(0, jdbcTemplate.queryForObject("SELECT COUNT(*) FROM media_asset", Integer.class));
        assertEquals(0, fileCount());
    }

    @Test
    void anonymous_asset_upload_returns_json_401() throws Exception {
        MockMultipartFile upload = new MockMultipartFile("file", "logo.png", MediaType.IMAGE_PNG_VALUE, pngBytes(8, 8));

        mockMvc.perform(multipart("/api/v1/admin/branding/assets").file(upload))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error").value("unauthorized"));
    }

    @Test
    @WithMockUser(username = "staff", authorities = "USER")
    void authenticated_non_admin_cannot_upload_assets() throws Exception {
        MockMultipartFile upload = new MockMultipartFile("file", "logo.png", MediaType.IMAGE_PNG_VALUE, pngBytes(8, 8));

        mockMvc.perform(multipart("/api/v1/admin/branding/assets").file(upload))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error").value("forbidden"));
        assertEquals(0, fileCount());
    }

    @Test
    @WithBrandingPermissions
    void asset_is_private_until_published_then_served_with_validated_headers() throws Exception {
        // Arrange
        byte[] png = pngBytes(24, 16);
        String responseBody = mockMvc.perform(multipart("/api/v1/admin/branding/assets")
                        .file(new MockMultipartFile("file", "brand.png", MediaType.IMAGE_PNG_VALUE, png)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        String assetId = assetIdFrom(responseBody);

        // Act + Assert: private uploads are not public before a configuration references them.
        mockMvc.perform(get("/assets/{assetId}", assetId)).andExpect(status().isNotFound());

        mockMvc.perform(put("/api/v1/admin/branding")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validBrandingChange(assetId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.revision").value(2));

        mockMvc.perform(get("/assets/{assetId}", assetId))
                .andExpect(status().isOk())
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.content().contentType(MediaType.IMAGE_PNG))
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.content().bytes(png))
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.header().string("X-Content-Type-Options", "nosniff"))
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.header().string("ETag", "\"" + sha256(png) + "\""))
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.header().string("Cache-Control", org.hamcrest.Matchers.containsString("immutable")));
    }

    @Test
    void rejects_unknown_or_noncanonical_public_asset_ids() throws Exception {
        mockMvc.perform(get("/assets/{assetId}", "not-a-uuid"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("asset_not_found"));
    }

    @Test
    @WithBrandingPermissions
    void does_not_serve_a_published_asset_when_stored_bytes_fail_the_checksum() throws Exception {
        // Arrange
        byte[] png = pngBytes(24, 16);
        String responseBody = mockMvc.perform(multipart("/api/v1/admin/branding/assets")
                        .file(new MockMultipartFile("file", "brand.png", MediaType.IMAGE_PNG_VALUE, png)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        String assetId = assetIdFrom(responseBody);
        String storageKey = jdbcTemplate.queryForObject(
                "SELECT storage_key FROM media_asset WHERE asset_id = ?", String.class, assetId
        );
        mockMvc.perform(put("/api/v1/admin/branding")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validBrandingChange(assetId)))
                .andExpect(status().isOk());
        byte[] corrupted = png.clone();
        corrupted[0] = (byte) (corrupted[0] ^ 0x01);
        Files.write(assetDirectory.resolve(storageKey), corrupted);

        // Act + Assert
        mockMvc.perform(get("/assets/{assetId}", assetId))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.error").value("asset_storage_unavailable"));
    }

    @Test
    @WithBrandingPermissions
    void serves_a_banner_asset_only_during_its_configured_active_window() throws Exception {
        // Arrange
        byte[] png = pngBytes(20, 12);
        String responseBody = mockMvc.perform(multipart("/api/v1/admin/branding/assets")
                        .file(new MockMultipartFile("file", "banner.png", MediaType.IMAGE_PNG_VALUE, png)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        String assetId = assetIdFrom(responseBody);
        Instant now = Instant.now();

        // Act + Assert: a future banner is not publicly retrievable yet.
        mockMvc.perform(put("/api/v1/admin/branding")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(brandingChangeWithBanner(assetId, 1, now.plusSeconds(3600), now.plusSeconds(7200))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.revision").value(2));
        mockMvc.perform(get("/assets/{assetId}", assetId)).andExpect(status().isNotFound());

        // Act + Assert: a newly published revision makes the same immutable asset public in its active window.
        mockMvc.perform(put("/api/v1/admin/branding")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(brandingChangeWithBanner(assetId, 2, now.minusSeconds(3600), now.plusSeconds(3600))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.revision").value(3));
        mockMvc.perform(get("/assets/{assetId}", assetId))
                .andExpect(status().isOk())
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.content().bytes(png));
    }

    @Test
    void database_metadata_failure_removes_the_temporary_asset_file() throws Exception {
        // Arrange: the existing media_asset table limits uploaded_by to 180 characters.
        String subject = "a".repeat(181);
        MockMultipartFile upload = new MockMultipartFile("file", "brand.png", MediaType.IMAGE_PNG_VALUE, pngBytes(16, 16));

        // Act + Assert
        mockMvc.perform(multipart("/api/v1/admin/branding/assets")
                        .file(upload)
                        .with(SecurityMockMvcRequestPostProcessors.jwt()
                                .jwt(jwt -> jwt.subject(subject))
                                .authorities(new SimpleGrantedAuthority("branding:write"))))
                .andExpect(status().isInternalServerError());
        assertEquals(0, jdbcTemplate.queryForObject("SELECT COUNT(*) FROM media_asset", Integer.class));
        assertEquals(0, jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM administrative_audit_event WHERE entity_type = 'media-asset'",
                Integer.class
        ));
        assertEquals(0, fileCount());
        assertFalse(Files.exists(assetDirectory.resolve("../brand.png")));
    }

    private int fileCount() throws IOException {
        try (var files = Files.list(assetDirectory)) {
            return Math.toIntExact(files.count());
        }
    }

    private static String assetIdFrom(String responseBody) {
        var matcher = java.util.regex.Pattern.compile("\\\"assetId\\\":\\\"([^\\\"]+)\\\"").matcher(responseBody);
        if (!matcher.find()) {
            throw new AssertionError("Response does not contain an opaque assetId.");
        }
        return matcher.group(1);
    }

    private static byte[] pngBytes(int width, int height) throws IOException {
        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        try (ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            ImageIO.write(image, "png", output);
            return output.toByteArray();
        }
    }

    private static byte[] jpegBytes(int width, int height) throws IOException {
        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        try (ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            ImageIO.write(image, "jpeg", output);
            return output.toByteArray();
        }
    }

    private static byte[] webpBytes() throws IOException {
        try (var input = BrandingAssetControllerTest.class.getResourceAsStream("/images/small_1x1.webp")) {
            if (input == null) {
                throw new AssertionError("Missing test WebP fixture.");
            }
            return input.readAllBytes();
        }
    }

    private static String sha256(byte[] bytes) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes));
        } catch (NoSuchAlgorithmException exception) {
            throw new AssertionError("SHA-256 is required by the Java runtime.", exception);
        }
    }

    private static String validBrandingChange(String logoAssetId) {
        return """
                {
                  "expectedRevision": 1,
                  "institutionName": "Universidad UPTC",
                  "colors": {
                    "primary": "#FFCC29", "ink": "#1A1A1A", "surface": "#FFFFFF",
                    "text": "#1A1A1A", "accent": "#FFCC29", "focus": "#1A1A1A"
                  },
                  "assets": {"logoLight": "%s", "logoDark": null, "favicon": null},
                  "modules": [
                    {"key":"home","label":"Inicio","available":true,"visible":true,"order":10},
                    {"key":"students","label":"Estudiantes","available":false,"visible":false,"order":20},
                    {"key":"programs","label":"Programas","available":false,"visible":false,"order":30},
                    {"key":"curricula","label":"Mallas curriculares","available":false,"visible":false,"order":40},
                    {"key":"subjects","label":"Asignaturas","available":false,"visible":false,"order":50},
                    {"key":"academic-load","label":"Carga académica","available":false,"visible":false,"order":60},
                    {"key":"spaces","label":"Guía de espacios","available":true,"visible":true,"order":70},
                    {"key":"visual-identity","label":"Identidad visual","available":true,"visible":true,"order":90},
                    {"key":"admissions","label":"Admisiones","available":true,"visible":true,"order":100}
                  ],
                  "banners": []
                }
                """.formatted(logoAssetId);
    }

    private static String brandingChangeWithBanner(String assetId, long expectedRevision, Instant startsAt, Instant endsAt) {
        String withRevision = validBrandingChange(assetId)
                .replace("\"expectedRevision\": 1", "\"expectedRevision\": " + expectedRevision)
                .replace("\"logoLight\": \"" + assetId + "\"", "\"logoLight\": null")
                .replace("\"banners\": []", "\"banners\": [{"
                        + "\"id\":\"" + UUID.randomUUID() + "\","
                        + "\"assetId\":\"" + assetId + "\","
                        + "\"title\":\"Banner institucional\","
                        + "\"altText\":\"Comunidad universitaria en el campus\","
                        + "\"placement\":\"home-hero\","
                        + "\"order\":1,"
                        + "\"startsAt\":\"" + startsAt + "\","
                        + "\"endsAt\":\"" + endsAt + "\"}]");
        return withRevision;
    }
}
