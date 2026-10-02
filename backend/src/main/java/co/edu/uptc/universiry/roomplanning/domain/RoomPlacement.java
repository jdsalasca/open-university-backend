package co.edu.uptc.universiry.roomplanning.domain;

import java.util.Objects;

public record RoomPlacement(
        String groupCode,
        RoomAssignmentStatus status,
        String roomCode,
        Integer remainingSeats,
        RoomUnassignmentReason reason
) {

    public RoomPlacement {
        Objects.requireNonNull(groupCode, "Group code is required.");
        Objects.requireNonNull(status, "Assignment status is required.");
        if (status == RoomAssignmentStatus.ASSIGNED) {
            if (roomCode == null || roomCode.isBlank() || remainingSeats == null || remainingSeats < 0 || reason != null) {
                throw new IllegalArgumentException("Assigned placement values are inconsistent.");
            }
        } else if (roomCode != null || remainingSeats != null || reason == null) {
            throw new IllegalArgumentException("Unassigned placement values are inconsistent.");
        }
    }
}
