package co.edu.uptc.universiry.territories.infrastructure.catalog;

import co.edu.uptc.universiry.territories.application.TerritorialCatalog;
import co.edu.uptc.universiry.territories.domain.TerritorialCatalogSnapshot;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.io.InputStream;

@Component
public final class ClasspathTerritorialCatalogAdapter implements TerritorialCatalog {

    private static final String CATALOG_PATH = "territories/divipola-mgn-2025.json";

    private final TerritorialCatalogSnapshot snapshot;

    public ClasspathTerritorialCatalogAdapter(ObjectMapper objectMapper) {
        this.snapshot = loadSnapshot(objectMapper);
    }

    @Override
    public TerritorialCatalogSnapshot snapshot() {
        return snapshot;
    }

    private TerritorialCatalogSnapshot loadSnapshot(ObjectMapper objectMapper) {
        ClassPathResource resource = new ClassPathResource(CATALOG_PATH);
        try (InputStream input = resource.getInputStream()) {
            return objectMapper.readValue(input, TerritorialCatalogSnapshot.class);
        } catch (IOException | IllegalArgumentException exception) {
            throw new IllegalStateException("Cannot load the versioned DANE territorial catalog snapshot.", exception);
        }
    }
}
