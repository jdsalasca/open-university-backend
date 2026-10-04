package co.edu.uptc.universiry.spaces.domain;

public record SpaceCapacityAnnouncement(String areaName, int announcedCapacityPersons) {

    public SpaceCapacityAnnouncement {
        if (areaName == null || areaName.isBlank()) {
            throw new IllegalArgumentException("Announced area name must not be blank.");
        }
        if (announcedCapacityPersons <= 0) {
            throw new IllegalArgumentException("Announced capacity must be positive.");
        }
    }
}
