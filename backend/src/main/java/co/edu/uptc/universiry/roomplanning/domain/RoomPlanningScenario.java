package co.edu.uptc.universiry.roomplanning.domain;

import java.util.HashSet;
import java.util.List;
import java.util.Objects;

public record RoomPlanningScenario(List<AcademicGroup> groups, List<RoomResource> rooms) {

    public RoomPlanningScenario {
        Objects.requireNonNull(groups, "At least one group is required.");
        groups = List.copyOf(groups);
        Objects.requireNonNull(rooms, "At least one room is required.");
        rooms = List.copyOf(rooms);
        if (groups.isEmpty() || groups.size() > RoomPlanningLimits.MAX_GROUPS) {
            throw new InvalidRoomPlanningScenarioException("Group count is outside the supported range.");
        }
        if (rooms.isEmpty() || rooms.size() > RoomPlanningLimits.MAX_ROOMS) {
            throw new InvalidRoomPlanningScenarioException("Room count is outside the supported range.");
        }
        if (groups.stream().anyMatch(Objects::isNull) || rooms.stream().anyMatch(Objects::isNull)) {
            throw new InvalidRoomPlanningScenarioException("Groups and rooms cannot be null.");
        }
        if (new HashSet<>(groups.stream().map(AcademicGroup::code).toList()).size() != groups.size()) {
            throw new InvalidRoomPlanningScenarioException("Group codes must be unique.");
        }
        if (new HashSet<>(rooms.stream().map(RoomResource::code).toList()).size() != rooms.size()) {
            throw new InvalidRoomPlanningScenarioException("Room codes must be unique.");
        }
    }
}
