package co.edu.uptc.universiry.branding.infrastructure.assets;

import co.edu.uptc.universiry.branding.application.AssetStorage;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.util.UUID;

@Component
public class LocalAssetStorage implements AssetStorage {

    private static final String GENERATED_KEY = "[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}\\.asset";

    private final Path root;

    public LocalAssetStorage(@Value("${branding.assets.directory:./.data/branding-assets}") String directory) {
        this.root = Path.of(directory).toAbsolutePath().normalize();
    }

    @Override
    public void write(String storageKey, byte[] content) throws IOException {
        Path destination = resolveGeneratedKey(storageKey);
        Files.createDirectories(root);
        Path temporary = Files.createTempFile(root, ".upload-", ".tmp");
        try {
            Files.write(temporary, content, StandardOpenOption.WRITE, StandardOpenOption.TRUNCATE_EXISTING);
            Files.move(temporary, destination, StandardCopyOption.ATOMIC_MOVE);
        } catch (IOException | RuntimeException exception) {
            try {
                Files.deleteIfExists(temporary);
            } catch (IOException cleanupFailure) {
                exception.addSuppressed(cleanupFailure);
            }
            throw exception;
        }
    }

    @Override
    public byte[] read(String storageKey, long maxBytes) throws IOException {
        Path file = resolveGeneratedKey(storageKey);
        if (maxBytes <= 0 || maxBytes >= Integer.MAX_VALUE) {
            throw new IOException("The configured asset read limit is invalid.");
        }
        if (!Files.isRegularFile(file, LinkOption.NOFOLLOW_LINKS)) {
            throw new IOException("The generated asset file does not exist.");
        }
        if (Files.size(file) > maxBytes) {
            throw new IOException("The generated asset file exceeds its recorded size.");
        }
        try (var input = Files.newInputStream(file, LinkOption.NOFOLLOW_LINKS)) {
            byte[] content = input.readNBytes(Math.toIntExact(maxBytes + 1));
            if (content.length > maxBytes) {
                throw new IOException("The generated asset file exceeds its recorded size.");
            }
            return content;
        }
    }

    @Override
    public void delete(String storageKey) throws IOException {
        Files.deleteIfExists(resolveGeneratedKey(storageKey));
    }

    private Path resolveGeneratedKey(String storageKey) throws IOException {
        if (storageKey == null || !storageKey.matches(GENERATED_KEY)) {
            throw new IOException("The asset storage key is invalid.");
        }
        Path resolved = root.resolve(storageKey).normalize();
        if (!resolved.getParent().equals(root) || !UUID.fromString(storageKey.substring(0, 36)).toString().concat(".asset").equals(storageKey)) {
            throw new IOException("The asset storage key is invalid.");
        }
        return resolved;
    }
}
