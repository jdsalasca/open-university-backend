package co.edu.uptc.universiry.spaces.domain;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

public record SpaceDirectorySnapshot(List<SpaceLocation> locations, String officialOfficeDirectoryUrl) {

    public SpaceDirectorySnapshot {
        if (locations == null || locations.isEmpty()) {
            throw new IllegalArgumentException("Space directory must contain at least one location.");
        }
        locations = List.copyOf(locations);
        Set<String> ids = new HashSet<>();
        for (SpaceLocation location : locations) {
            if (location == null || !ids.add(location.id())) {
                throw new IllegalArgumentException("Space directory location ids must be unique and non-null.");
            }
        }
        if (!SpaceSource.isHttpsUrl(officialOfficeDirectoryUrl)) {
            throw new IllegalArgumentException("Official office directory URL must use HTTPS.");
        }
    }
}
