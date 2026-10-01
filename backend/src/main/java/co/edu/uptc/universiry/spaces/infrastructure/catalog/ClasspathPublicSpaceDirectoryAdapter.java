package co.edu.uptc.universiry.spaces.infrastructure.catalog;

import co.edu.uptc.universiry.spaces.application.PublicSpaceDirectory;
import co.edu.uptc.universiry.spaces.domain.SpaceDirectorySnapshot;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.io.InputStream;

@Component
public final class ClasspathPublicSpaceDirectoryAdapter implements PublicSpaceDirectory {

    private static final String CATALOG_PATH = "spaces/public-space-directory.json";

    private final SpaceDirectorySnapshot snapshot;

    public ClasspathPublicSpaceDirectoryAdapter(ObjectMapper objectMapper) {
        this.snapshot = loadSnapshot(objectMapper);
    }

    @Override
    public SpaceDirectorySnapshot snapshot() {
        return snapshot;
    }

    private SpaceDirectorySnapshot loadSnapshot(ObjectMapper objectMapper) {
        ClassPathResource resource = new ClassPathResource(CATALOG_PATH);
        try (InputStream input = resource.getInputStream()) {
            return objectMapper.readValue(input, SpaceDirectorySnapshot.class);
        } catch (IOException | IllegalArgumentException exception) {
            throw new IllegalStateException("Cannot load the versioned public space directory.", exception);
        }
    }
}
