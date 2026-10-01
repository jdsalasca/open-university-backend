package co.edu.uptc.universiry.spaces.domain;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

public record SpaceDirectorySnapshot(
        List<SpaceLocation> locations,
        List<SpaceUsePathway> requestPathways,
        String officialOfficeDirectoryUrl
) {

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
        if (requestPathways == null || requestPathways.isEmpty()) {
            throw new IllegalArgumentException("Space request pathways must contain at least one entry.");
        }
        requestPathways = List.copyOf(requestPathways);
        Set<String> pathwayIds = new HashSet<>();
        for (SpaceUsePathway pathway : requestPathways) {
            if (pathway == null || !pathwayIds.add(pathway.id())) {
                throw new IllegalArgumentException("Space request pathway ids must be unique and non-null.");
            }
        }
        if (!SpaceSource.isHttpsUrl(officialOfficeDirectoryUrl)) {
            throw new IllegalArgumentException("Official office directory URL must use HTTPS.");
        }
    }
}
