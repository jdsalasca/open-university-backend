package co.edu.uptc.universiry.spaces.infrastructure.catalog;

import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PublicSpaceDirectoryCatalogContractTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void publishes_twenty_one_locations_with_unique_ids_and_traceable_sources() throws IOException {
        JsonNode snapshot = readSnapshot();
        JsonNode locations = snapshot.path("locations");

        assertEquals(21, locations.size());

        Set<String> ids = new HashSet<>();
        int campuses = 0;
        int creadLocations = 0;
        int services = 0;
        for (JsonNode location : locations) {
            assertTrue(ids.add(location.path("id").asText()), "each location id must be unique");
            assertFalse(location.path("name").asText().isBlank());
            assertFalse(location.path("source").path("label").asText().isBlank());
            assertTrue(location.path("source").path("url").asText().startsWith("https://"));
            assertEquals("2026-10-01", location.path("source").path("checkedAt").asText());
            switch (location.path("kind").asText()) {
                case "CAMPUS", "REGIONAL_SITE" -> campuses++;
                case "CREAD" -> creadLocations++;
                case "SERVICE" -> services++;
                default -> throw new AssertionError("unexpected location kind: " + location.path("kind"));
            }
        }

        assertEquals(6, campuses);
        assertEquals(11, creadLocations);
        assertEquals(4, services);
        assertEquals("https://www.uptc.edu.co/sitio/portal/sitios/directorio/",
                snapshot.path("officialOfficeDirectoryUrl").asText());
    }

    @Test
    void does_not_infer_unpublished_cread_departments_and_keeps_unpublished_update_date_null() throws IOException {
        JsonNode snapshot = readSnapshot();
        JsonNode bogotaCread = findById(snapshot, "cread-bogota");
        JsonNode acra = findById(snapshot, "service-acra");
        JsonNode rondonCread = findById(snapshot, "cread-rondon");

        assertTrue(bogotaCread.has("department"));
        assertTrue(bogotaCread.get("department").isNull());
        assertTrue(acra.path("source").has("sourceUpdatedAt"));
        assertNull(acra.path("source").get("sourceUpdatedAt").textValue());
        assertFalse(acra.path("address").asText().isBlank());
        assertFalse(acra.path("locationDetail").asText().isBlank());
        assertTrue(rondonCread.get("address").isNull());
        assertTrue(rondonCread.get("mapQuery").isNull());
        assertFalse(rondonCread.path("locationDetail").asText().isBlank());
    }

    @Test
    void publishes_distinct_sourced_request_pathways_without_claiming_live_availability() throws IOException {
        JsonNode snapshot = readSnapshot();
        JsonNode pathways = snapshot.path("requestPathways");

        assertEquals(5, pathways.size());

        Set<String> ids = new HashSet<>();
        Set<String> kinds = new HashSet<>();
        for (JsonNode pathway : pathways) {
            assertTrue(ids.add(pathway.path("id").asText()), "each pathway id must be unique");
            assertFalse(pathway.path("title").asText().isBlank());
            assertFalse(pathway.path("audience").asText().isBlank());
            assertFalse(pathway.path("summary").asText().isBlank());
            assertFalse(pathway.path("availabilityNote").asText().isBlank());
            assertTrue(pathway.path("sources").size() > 0);
            for (JsonNode source : pathway.path("sources")) {
                assertTrue(source.path("url").asText().startsWith("https://"));
                assertEquals("2026-10-01", source.path("checkedAt").asText());
            }
            kinds.add(pathway.path("kind").asText());
        }

        assertEquals(Set.of("AUDITORIUM_OR_ACADEMIC_SPACE", "SPORTS_VENUE", "LIBRARY_ROOM",
                "COMPUTER_CLASSROOM", "INTERNAL_STAFF_SPACE"), kinds);
    }

    private JsonNode readSnapshot() throws IOException {
        ClassPathResource resource = new ClassPathResource("spaces/public-space-directory.json");
        assertTrue(resource.exists(), "the sourced public space directory must be packaged with the application");
        try (InputStream input = resource.getInputStream()) {
            JsonNode snapshot = objectMapper.readTree(input);
            assertNotNull(snapshot);
            return snapshot;
        }
    }

    private JsonNode findById(JsonNode snapshot, String id) {
        for (JsonNode location : snapshot.path("locations")) {
            if (id.equals(location.path("id").asText())) {
                return location;
            }
        }
        throw new AssertionError("location not found: " + id);
    }
}
