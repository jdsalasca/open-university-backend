package co.edu.uptc.universiry.branding.domain;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class BrandingConfigurationTest {

    @Test
    void rejects_non_hex_color() {
        // Arrange
        List<String> invalidValues = List.of(
                "javascript:alert(1)", "#FFF", "#11223344", " #123456", "#12G456"
        );

        // Act + Assert
        invalidValues.forEach(value -> assertThrows(IllegalArgumentException.class, () -> BrandColor.fromHex(value)));
        assertThrows(IllegalArgumentException.class, () -> BrandColor.fromHex(null));
    }

    @Test
    void accepts_hex_color_and_normalizes_case() {
        // Arrange
        String lowercaseHex = "#a1b2c3";

        // Act
        BrandColor color = BrandColor.fromHex(lowercaseHex);

        // Assert
        assertEquals("#A1B2C3", color.hex());
    }

    @Test
    void default_catalog_has_stable_keys() {
        // Arrange
        List<String> expectedKeys = List.of(
                "home", "students", "programs", "curricula", "subjects", "academic-load", "visual-identity"
        );

        // Act
        List<String> actualKeys = BrandModule.defaultCatalog().stream().map(BrandModule::key).toList();

        // Assert
        assertEquals(expectedKeys, actualKeys);
        assertEquals(actualKeys.size(), actualKeys.stream().distinct().count());
    }
}
