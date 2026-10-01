package co.edu.uptc.universiry.spaces.domain;

import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SpaceDirectorySnapshotTest {

    @Test
    void rejects_an_empty_directory() {
        assertThrows(IllegalArgumentException.class,
                () -> new SpaceDirectorySnapshot(List.of(), "https://example.edu/offices"));
    }

    @Test
    void rejects_duplicate_location_ids() {
        SpaceLocation location = location("tunja", null);

        assertThrows(IllegalArgumentException.class,
                () -> new SpaceDirectorySnapshot(List.of(location, location), "https://example.edu/offices"));
    }

    @Test
    void copies_locations_and_preserves_missing_department_and_source_update_date() {
        List<SpaceLocation> locations = new ArrayList<>(List.of(location("bogota", null)));
        SpaceDirectorySnapshot snapshot = new SpaceDirectorySnapshot(locations, "https://example.edu/offices");

        locations.clear();

        assertEquals(1, snapshot.locations().size());
        assertEquals(null, snapshot.locations().getFirst().department());
        assertEquals(null, snapshot.locations().getFirst().source().sourceUpdatedAt());
        assertThrows(UnsupportedOperationException.class,
                () -> snapshot.locations().add(location("other", null)));
    }

    @Test
    void accepts_a_declared_source_update_date() {
        SpaceSource source = new SpaceSource("UPTC", "https://uptc.edu.co/source",
                LocalDate.of(2026, 10, 1), LocalDate.of(2026, 7, 3));

        assertTrue(source.sourceUpdatedAt().isBefore(source.checkedAt()));
    }

    private SpaceLocation location(String id, String department) {
        return new SpaceLocation(id, SpaceLocationKind.CREAD, id, "Bogotá", department,
                "Carrera 13 No. 24-15", "Instalaciones INCCA", "Bogotá UPTC",
                new SpaceSource("UPTC", "https://uptc.edu.co/source",
                        LocalDate.of(2026, 10, 1), null));
    }
}
