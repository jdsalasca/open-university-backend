package co.edu.uptc.universiry.roomplanning.domain;

import java.util.List;
import java.util.Objects;
import java.util.Set;

public record AcademicGroup(
        String code,
        int expectedEnrollment,
        Set<RoomFeature> requiredFeatures,
        List<RoomMeeting> meetings
) {

    public AcademicGroup {
        code = requireCode(code, "Group");
        if (expectedEnrollment < 1 || expectedEnrollment > RoomPlanningLimits.MAX_PARTICIPANTS) {
            throw new InvalidRoomPlanningScenarioException("Group enrollment is outside the supported range.");
        }
        Objects.requireNonNull(requiredFeatures, "Required room features are required.");
        requiredFeatures = Set.copyOf(requiredFeatures);
        Objects.requireNonNull(meetings, "Group meetings are required.");
        meetings = List.copyOf(meetings);
        if (meetings.isEmpty() || meetings.size() > RoomPlanningLimits.MAX_MEETINGS_PER_GROUP) {
            throw new InvalidRoomPlanningScenarioException("Group meeting count is outside the supported range.");
        }
        if (meetings.stream().anyMatch(Objects::isNull)) {
            throw new InvalidRoomPlanningScenarioException("Group meetings cannot be null.");
        }
        for (int first = 0; first < meetings.size(); first++) {
            for (int second = first + 1; second < meetings.size(); second++) {
                if (meetings.get(first).overlaps(meetings.get(second))) {
                    throw new InvalidRoomPlanningScenarioException("Meetings inside one group cannot overlap.");
                }
            }
        }
    }

    private static String requireCode(String value, String label) {
        Objects.requireNonNull(value, label + " code is required.");
        String normalized = value.strip();
        if (normalized.isEmpty() || normalized.length() > RoomPlanningLimits.MAX_CODE_LENGTH) {
            throw new InvalidRoomPlanningScenarioException(label + " code is outside the supported range.");
        }
        return normalized;
    }
}
