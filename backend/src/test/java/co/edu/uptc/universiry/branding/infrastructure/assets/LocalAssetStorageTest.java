package co.edu.uptc.universiry.branding.infrastructure.assets;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class LocalAssetStorageTest {

    @TempDir
    Path directory;

    @Test
    void read_is_bounded_by_the_persisted_asset_size() throws Exception {
        // Arrange
        LocalAssetStorage storage = new LocalAssetStorage(directory.toString());
        String storageKey = UUID.randomUUID() + ".asset";
        byte[] content = {1, 2, 3, 4};
        storage.write(storageKey, content);

        // Act + Assert
        assertThrows(IOException.class, () -> storage.read(storageKey, content.length - 1L));
        assertArrayEquals(content, storage.read(storageKey, content.length));
    }

    @Test
    void rejects_storage_keys_that_could_escape_the_asset_directory() throws Exception {
        // Arrange
        LocalAssetStorage storage = new LocalAssetStorage(directory.toString());
        Path outside = directory.getParent().resolve("outside.asset");

        // Act + Assert
        assertThrows(IOException.class, () -> storage.write("../outside.asset", new byte[]{1}));
        assertThrows(IOException.class, () -> storage.read("../outside.asset", 10));
        assertThrows(IOException.class, () -> storage.delete("../outside.asset"));
        org.junit.jupiter.api.Assertions.assertFalse(Files.exists(outside));
    }
}
