package co.edu.uptc.universiry.branding.domain;

import java.util.Locale;
import java.util.regex.Pattern;

public record BrandColor(String hex) {

    private static final Pattern HEX_COLOR = Pattern.compile("^#[0-9A-Fa-f]{6}$");

    public BrandColor {
        if (hex == null || !HEX_COLOR.matcher(hex).matches()) {
            throw new IllegalArgumentException("Brand colors must use six-digit hexadecimal notation.");
        }

        hex = hex.toUpperCase(Locale.ROOT);
    }

    public static BrandColor fromHex(String value) {
        return new BrandColor(value);
    }
}
