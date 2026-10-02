package co.edu.uptc.universiry.roomplanning.infrastructure.web;

import co.edu.uptc.universiry.roomplanning.domain.RoomAssignmentStatus;
import co.edu.uptc.universiry.roomplanning.domain.RoomPlanningProposal;
import co.edu.uptc.universiry.roomplanning.domain.RoomUnassignmentReason;

import java.util.List;

public record RoomAllocationProposalResponse(
        int assignedGroups,
        int unassignedGroups,
        int unusedSeats,
        List<PlacementResponse> placements
) {

    public RoomAllocationProposalResponse {
        placements = List.copyOf(placements);
    }

    public static RoomAllocationProposalResponse from(RoomPlanningProposal proposal) {
        return new RoomAllocationProposalResponse(
                proposal.assignedGroups(), proposal.unassignedGroups(), proposal.unusedSeats(),
                proposal.placements().stream().map(PlacementResponse::from).toList()
        );
    }

    public record PlacementResponse(
            String groupCode,
            RoomAssignmentStatus status,
            String roomCode,
            Integer remainingSeats,
            RoomUnassignmentReason reason
    ) {

        static PlacementResponse from(co.edu.uptc.universiry.roomplanning.domain.RoomPlacement placement) {
            return new PlacementResponse(placement.groupCode(), placement.status(), placement.roomCode(),
                    placement.remainingSeats(), placement.reason());
        }
    }
}
