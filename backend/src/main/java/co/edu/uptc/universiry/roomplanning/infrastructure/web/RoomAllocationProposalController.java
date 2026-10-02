package co.edu.uptc.universiry.roomplanning.infrastructure.web;

import co.edu.uptc.universiry.roomplanning.application.RoomAssignmentPlanner;
import jakarta.validation.Valid;
import org.springframework.context.annotation.Profile;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Profile("local-preview")
public class RoomAllocationProposalController {

    private final RoomAssignmentPlanner planner;

    public RoomAllocationProposalController(RoomAssignmentPlanner planner) {
        this.planner = planner;
    }

    @PostMapping("/api/v1/dev/room-allocation/proposals")
    public RoomAllocationProposalResponse propose(@Valid @RequestBody RoomAllocationProposalRequest request) {
        return RoomAllocationProposalResponse.from(planner.propose(request.toScenario()));
    }
}
