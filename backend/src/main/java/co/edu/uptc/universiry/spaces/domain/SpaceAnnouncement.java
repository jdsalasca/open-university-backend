package co.edu.uptc.universiry.spaces.domain;

import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;

public record SpaceAnnouncement(
        List<SpaceCapacityAnnouncement> capacities,
        String locationNote,
        List<SpaceSource> locationReferences
) {

    public SpaceAnnouncement {
        capacities = List.copyOf(Objects.requireNonNull(capacities, "Announced capacities must not be null."));
        if (capacities.isEmpty()) {
            throw new IllegalArgumentException("An announcement must include at least one capacity.");
        }
        Set<String> areaNames = new HashSet<>();
        for (SpaceCapacityAnnouncement capacity : capacities) {
            Objects.requireNonNull(capacity, "Announced capacity must not be null.");
            if (!areaNames.add(capacity.areaName().trim().toLowerCase(Locale.ROOT))) {
                throw new IllegalArgumentException("An announcement must not repeat an area name.");
            }
        }
        if (locationNote == null || locationNote.isBlank()) {
            throw new IllegalArgumentException("Announcement location note must not be blank.");
        }
        locationReferences = List.copyOf(
                Objects.requireNonNull(locationReferences, "Location references must not be null."));
        if (locationReferences.isEmpty()) {
            throw new IllegalArgumentException("An announcement must include location references.");
        }
        Set<String> referenceUrls = new HashSet<>();
        for (SpaceSource reference : locationReferences) {
            Objects.requireNonNull(reference, "Location reference must not be null.");
            if (!referenceUrls.add(reference.url())) {
                throw new IllegalArgumentException("An announcement must not repeat a location reference.");
            }
        }
    }
}
