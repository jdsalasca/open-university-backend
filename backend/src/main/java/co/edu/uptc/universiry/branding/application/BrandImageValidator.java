package co.edu.uptc.universiry.branding.application;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.stream.ImageInputStream;
import javax.imageio.stream.MemoryCacheImageInputStream;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.Iterator;

@Component
public class BrandImageValidator {

    private static final byte[] PNG_SIGNATURE = {(byte) 0x89, 0x50, 0x4e, 0x47, 0x0d, 0x0a, 0x1a, 0x0a};

    private final long maxBytes;
    private final int maxWidth;
    private final int maxHeight;
    private final long maxPixels;

    public BrandImageValidator(
            @Value("${branding.assets.max-bytes:5242880}") long maxBytes,
            @Value("${branding.assets.max-width:8192}") int maxWidth,
            @Value("${branding.assets.max-height:8192}") int maxHeight,
            @Value("${branding.assets.max-pixels:16777216}") long maxPixels
    ) {
        if (maxBytes <= 0 || maxWidth <= 0 || maxHeight <= 0 || maxPixels <= 0) {
            throw new IllegalArgumentException("Image upload limits must be positive.");
        }
        this.maxBytes = maxBytes;
        this.maxWidth = maxWidth;
        this.maxHeight = maxHeight;
        this.maxPixels = maxPixels;
    }

    public ValidatedImage validate(byte[] bytes) {
        if (bytes == null || bytes.length == 0) {
            throw new BrandAssetValidationException("Choose a non-empty PNG, JPEG, or WebP image.");
        }
        if (bytes.length > maxBytes) {
            throw new BrandAssetTooLargeException();
        }

        String mimeType = detectMimeType(bytes);
        try (ImageInputStream input = new MemoryCacheImageInputStream(new ByteArrayInputStream(bytes))) {
            ImageReader reader = supportedReader(input, mimeType);
            if (reader == null) {
                throw new BrandAssetValidationException("The uploaded bytes are not a supported image.");
            }

            BufferedImage decoded = null;
            try {
                reader.setInput(input, false, true);
                int width = reader.getWidth(0);
                int height = reader.getHeight(0);
                validateDimensions(width, height);
                if (reader.getNumImages(true) != 1) {
                    throw new BrandAssetValidationException("Animated or multi-frame images are not supported.");
                }
                decoded = reader.read(0);
                if (decoded == null || decoded.getWidth() != width || decoded.getHeight() != height) {
                    throw new BrandAssetValidationException("The uploaded image data is incomplete or invalid.");
                }
                return new ValidatedImage(mimeType, width, height);
            } finally {
                if (decoded != null) {
                    decoded.flush();
                }
                reader.dispose();
            }
        } catch (BrandAssetValidationException exception) {
            throw exception;
        } catch (IOException | RuntimeException exception) {
            throw new BrandAssetValidationException("The uploaded image data is incomplete or invalid.");
        }
    }

    private String detectMimeType(byte[] bytes) {
        if (startsWith(bytes, PNG_SIGNATURE)) {
            return "image/png";
        }
        if (bytes.length >= 3 && (bytes[0] & 0xff) == 0xff && (bytes[1] & 0xff) == 0xd8 && (bytes[2] & 0xff) == 0xff) {
            return "image/jpeg";
        }
        if (bytes.length >= 12 && ascii(bytes, 0, "RIFF") && ascii(bytes, 8, "WEBP")) {
            if (bytes.length < 20 || unsignedLittleEndianInt(bytes, 4) != bytes.length - 8L) {
                throw new BrandAssetValidationException("The WebP container length does not match its content.");
            }
            return "image/webp";
        }
        throw new BrandAssetValidationException("Only PNG, JPEG, and WebP image data is accepted.");
    }

    private ImageReader supportedReader(ImageInputStream input, String mimeType) throws IOException {
        Iterator<ImageReader> readers = ImageIO.getImageReaders(input);
        while (readers.hasNext()) {
            ImageReader reader = readers.next();
            String format;
            try {
                format = reader.getFormatName();
            } catch (IOException exception) {
                reader.dispose();
                throw exception;
            }
            boolean matches = switch (mimeType) {
                case "image/png" -> format.equalsIgnoreCase("png");
                case "image/jpeg" -> format.equalsIgnoreCase("jpeg") || format.equalsIgnoreCase("jpg");
                case "image/webp" -> format.equalsIgnoreCase("webp");
                default -> false;
            };
            if (matches) {
                return reader;
            }
            reader.dispose();
        }
        return null;
    }

    private void validateDimensions(int width, int height) {
        if (width <= 0 || height <= 0 || width > maxWidth || height > maxHeight || (long) width * height > maxPixels) {
            throw new BrandAssetValidationException("The image dimensions exceed the supported limits.");
        }
    }

    private static boolean startsWith(byte[] bytes, byte[] prefix) {
        if (bytes.length < prefix.length) {
            return false;
        }
        for (int index = 0; index < prefix.length; index++) {
            if (bytes[index] != prefix[index]) {
                return false;
            }
        }
        return true;
    }

    private static boolean ascii(byte[] bytes, int offset, String value) {
        for (int index = 0; index < value.length(); index++) {
            if (bytes[offset + index] != value.charAt(index)) {
                return false;
            }
        }
        return true;
    }

    private static long unsignedLittleEndianInt(byte[] bytes, int offset) {
        return (bytes[offset] & 0xffL)
                | ((bytes[offset + 1] & 0xffL) << 8)
                | ((bytes[offset + 2] & 0xffL) << 16)
                | ((bytes[offset + 3] & 0xffL) << 24);
    }

    public record ValidatedImage(String mimeType, int widthPx, int heightPx) {
    }
}
