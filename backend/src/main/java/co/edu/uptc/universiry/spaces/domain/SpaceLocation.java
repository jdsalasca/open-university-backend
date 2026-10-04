package co.edu.uptc.universiry.spaces.domain;

import java.util.Objects;

public record SpaceLocation(
        String id,
        SpaceLocationKind kind,
        String name,
        String municipality,
        String department,
        String address,
        String locationDetail,
        String mapQuery,
        SpaceSource source,
        SpaceAnnouncement announcement
) {

    public SpaceLocation {
        if (id == null || id.isBlank()) {
            throw new IllegalArgumentException("Location id must not be blank.");
        }
        Objects.requireNonNull(kind, "Location kind must not be null.");
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("Location name must not be blank.");
        }
        if (municipality == null || municipality.isBlank()) {
            throw new IllegalArgumentException("Location municipality must not be blank.");
        }
        if (address != null && address.isBlank()) {
            throw new IllegalArgumentException("Location address must not be blank when provided.");
        }
        if (mapQuery != null && mapQuery.isBlank()) {
            throw new IllegalArgumentException("Map query must not be blank when provided.");
        }
        if ((address == null) != (mapQuery == null)) {
            throw new IllegalArgumentException("A map query is available only for a published street address.");
        }
        if (address == null && (locationDetail == null || locationDetail.isBlank())) {
            throw new IllegalArgumentException("A location without a street address needs a published location detail.");
        }
        Objects.requireNonNull(source, "Location source must not be null.");
        if (announcement != null && (kind != SpaceLocationKind.SERVICE || address != null || mapQuery != null)) {
            throw new IllegalArgumentException(
                    "An announced service location must not imply a street address or map query.");
        }
    }
}
