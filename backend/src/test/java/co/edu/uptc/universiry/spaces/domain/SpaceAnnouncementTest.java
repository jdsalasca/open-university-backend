package co.edu.uptc.universiry.spaces.domain;

import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SpaceAnnouncementTest {

    @Test
    void rejects_zero_or_negative_announced_capacity() {
        // Arrange + Act + Assert
        assertThrows(IllegalArgumentException.class,
                () -> new SpaceCapacityAnnouncement("Sala de estudio", 0));
        assertThrows(IllegalArgumentException.class,
                () -> new SpaceCapacityAnnouncement("Sala de estudio", -1));
    }

    @Test
    void rejects_duplicate_area_names_without_case_or_whitespace_variants() {
        // Arrange
        List<SpaceCapacityAnnouncement> capacities = List.of(
                new SpaceCapacityAnnouncement("Sala de estudio", 25),
                new SpaceCapacityAnnouncement(" sala de estudio ", 30));

        // Act + Assert
        assertThrows(IllegalArgumentException.class,
                () -> new SpaceAnnouncement(capacities, "Confirma la ubicación.", List.of(source())));
    }

    @Test
    void copies_capacity_and_reference_lists() {
        // Arrange
        List<SpaceCapacityAnnouncement> capacities = new ArrayList<>(List.of(
                new SpaceCapacityAnnouncement("Sala de estudio", 25)));
        List<SpaceSource> references = new ArrayList<>(List.of(source()));

        // Act
        SpaceAnnouncement announcement = new SpaceAnnouncement(capacities, "Confirma la ubicación.", references);
        capacities.clear();
        references.clear();

        // Assert
        assertEquals(1, announcement.capacities().size());
        assertEquals(1, announcement.locationReferences().size());
        assertThrows(UnsupportedOperationException.class,
                () -> announcement.capacities().add(new SpaceCapacityAnnouncement("Otra sala", 8)));
    }

    @Test
    void rejects_announcements_on_locations_with_a_map_address() {
        // Arrange
        SpaceAnnouncement announcement = new SpaceAnnouncement(
                List.of(new SpaceCapacityAnnouncement("Sala de estudio", 25)),
                "Confirma la ubicación.", List.of(source()));

        // Act + Assert
        assertThrows(IllegalArgumentException.class, () -> new SpaceLocation(
                "service-music", SpaceLocationKind.SERVICE, "Biblioteca de Música", "Tunja", null,
                "Calle 1", "Edificio de Música", "Edificio de Música UPTC", source(), announcement));
    }

    @Test
    void accepts_a_valid_announced_service_without_a_street_address() {
        // Arrange
        SpaceAnnouncement announcement = new SpaceAnnouncement(
                List.of(new SpaceCapacityAnnouncement("Sala de estudio", 25)),
                "Confirma la ubicación.", List.of(source()));

        // Act
        SpaceLocation location = new SpaceLocation(
                "service-music", SpaceLocationKind.SERVICE, "Biblioteca de Música", "Tunja", null,
                null, "Segundo piso del Edificio de Música", null, source(), announcement);

        // Assert
        assertTrue(location.announcement().capacities().getFirst().announcedCapacityPersons() > 0);
    }

    private SpaceSource source() {
        return new SpaceSource("UPTC", "https://uptc.edu.co/source", LocalDate.of(2026, 10, 3), null);
    }
}
