package co.edu.uptc.universiry.admissions.domain;

import java.util.UUID;

public record AdmissionsActor(UUID userId, UUID identityId) {
    public AdmissionsActor {
        if (userId == null || identityId == null) {
            throw new IllegalArgumentException("canonical actor and registered identity are required");
        }
    }
}
