package co.edu.uptc.universiry.spaces.domain;

import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

public record SpaceUsePathway(
        String id,
        SpaceUseKind kind,
        String title,
        String audience,
        String summary,
        String availabilityNote,
        List<SpaceSource> sources
) {

    public SpaceUsePathway {
        if (id == null || id.isBlank()) {
            throw new IllegalArgumentException("Space use pathway id must not be blank.");
        }
        Objects.requireNonNull(kind, "Space use pathway kind must not be null.");
        if (title == null || title.isBlank()) {
            throw new IllegalArgumentException("Space use pathway title must not be blank.");
        }
        if (audience == null || audience.isBlank()) {
            throw new IllegalArgumentException("Space use pathway audience must not be blank.");
        }
        if (summary == null || summary.isBlank()) {
            throw new IllegalArgumentException("Space use pathway summary must not be blank.");
        }
        if (availabilityNote == null || availabilityNote.isBlank()) {
            throw new IllegalArgumentException("Space use pathway availability note must not be blank.");
        }
        if (sources == null || sources.isEmpty()) {
            throw new IllegalArgumentException("Space use pathway must have at least one source.");
        }
        sources = List.copyOf(sources);
        Set<String> sourceUrls = new HashSet<>();
        for (SpaceSource source : sources) {
            if (source == null || !sourceUrls.add(source.url())) {
                throw new IllegalArgumentException("Space use pathway sources must be non-null and unique.");
            }
        }
    }
}
