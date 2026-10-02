package co.edu.uptc.universiry.roomplanning.application;

import co.edu.uptc.universiry.roomplanning.domain.RoomPlanningProposal;
import co.edu.uptc.universiry.roomplanning.domain.RoomPlanningScenario;

public interface RoomAssignmentPlanner {

    RoomPlanningProposal propose(RoomPlanningScenario scenario);
}
