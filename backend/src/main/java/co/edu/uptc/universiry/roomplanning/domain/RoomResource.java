package co.edu.uptc.universiry.roomplanning.domain;

import java.util.Objects;
import java.util.Set;

public record RoomResource(String code, int capacity, boolean active, Set<RoomFeature> features) {

    public RoomResource {
        Objects.requireNonNull(code, "Room code is required.");
        code = code.strip();
        if (code.isEmpty() || code.length() > RoomPlanningLimits.MAX_CODE_LENGTH) {
            throw new InvalidRoomPlanningScenarioException("Room code is outside the supported range.");
        }
        if (capacity < 1 || capacity > RoomPlanningLimits.MAX_PARTICIPANTS) {
            throw new InvalidRoomPlanningScenarioException("Room capacity is outside the supported range.");
        }
        Objects.requireNonNull(features, "Room features are required.");
        features = Set.copyOf(features);
    }
}
