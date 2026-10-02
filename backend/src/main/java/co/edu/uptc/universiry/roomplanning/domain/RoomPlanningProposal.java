package co.edu.uptc.universiry.roomplanning.domain;

import java.util.List;
import java.util.Objects;

public record RoomPlanningProposal(
        List<RoomPlacement> placements,
        int assignedGroups,
        int unassignedGroups,
        int unusedSeats
) {

    public RoomPlanningProposal {
        Objects.requireNonNull(placements, "Placements are required.");
        placements = List.copyOf(placements);
        if (assignedGroups < 0 || unassignedGroups < 0 || unusedSeats < 0
                || assignedGroups + unassignedGroups != placements.size()) {
            throw new IllegalArgumentException("Proposal totals are inconsistent.");
        }
    }
}
