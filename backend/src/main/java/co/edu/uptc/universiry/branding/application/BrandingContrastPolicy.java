package co.edu.uptc.universiry.branding.application;

import co.edu.uptc.universiry.branding.domain.BrandColor;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
public class BrandingContrastPolicy {

    public void validate(Map<String, BrandColor> colors) {
        requireContrast(colors, "text", "surface", 4.5);
        requireContrast(colors, "ink", "primary", 4.5);
        requireContrast(colors, "focus", "surface", 3.0);
        requireContrast(colors, "focus", "primary", 3.0);
    }

    public double contrastRatio(BrandColor first, BrandColor second) {
        double firstLuminance = relativeLuminance(first);
        double secondLuminance = relativeLuminance(second);
        double lighter = Math.max(firstLuminance, secondLuminance);
        double darker = Math.min(firstLuminance, secondLuminance);
        return (lighter + 0.05) / (darker + 0.05);
    }

    private void requireContrast(Map<String, BrandColor> colors, String foreground, String background, double minimum) {
        double ratio = contrastRatio(colors.get(foreground), colors.get(background));
        if (ratio < minimum) {
            throw new BrandingValidationException(
                    "The " + foreground + " color needs a contrast ratio of at least " + minimum + ":1 against " + background + "."
            );
        }
    }

    private double relativeLuminance(BrandColor color) {
        String hex = color.hex().substring(1);
        double red = linearChannel(Integer.parseInt(hex.substring(0, 2), 16));
        double green = linearChannel(Integer.parseInt(hex.substring(2, 4), 16));
        double blue = linearChannel(Integer.parseInt(hex.substring(4, 6), 16));
        return 0.2126 * red + 0.7152 * green + 0.0722 * blue;
    }

    private double linearChannel(int channel) {
        double normalized = channel / 255.0;
        return normalized <= 0.04045 ? normalized / 12.92 : Math.pow((normalized + 0.055) / 1.055, 2.4);
    }
}
