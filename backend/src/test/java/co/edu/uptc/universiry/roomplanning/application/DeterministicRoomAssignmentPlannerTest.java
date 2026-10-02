package co.edu.uptc.universiry.roomplanning.application;

import co.edu.uptc.universiry.roomplanning.domain.RoomAssignmentStatus;
import co.edu.uptc.universiry.roomplanning.domain.RoomPlanningScenario;
import co.edu.uptc.universiry.roomplanning.domain.RoomResource;
import co.edu.uptc.universiry.roomplanning.domain.RoomFeature;
import co.edu.uptc.universiry.roomplanning.domain.RoomMeeting;
import co.edu.uptc.universiry.roomplanning.domain.RoomPlacement;
import co.edu.uptc.universiry.roomplanning.domain.RoomUnassignmentReason;
import co.edu.uptc.universiry.roomplanning.domain.AcademicGroup;
import co.edu.uptc.universiry.roomplanning.domain.InvalidRoomPlanningScenarioException;
import org.junit.jupiter.api.Test;

import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class DeterministicRoomAssignmentPlannerTest {

    private final DeterministicRoomAssignmentPlanner planner = new DeterministicRoomAssignmentPlanner();

    @Test
    void maximizes_placed_groups_and_uses_the_smallest_eligible_rooms() {
        // Arrange
        RoomPlanningScenario scenario = new RoomPlanningScenario(
                List.of(
                        group("DEMO-FLEX", 18, Set.of(), meeting(DayOfWeek.TUESDAY, "08:00", "10:00")),
                        group("DEMO-LAB", 22, Set.of(RoomFeature.COMPUTER_STATIONS), meeting(DayOfWeek.TUESDAY, "08:00", "10:00"))
                ),
                List.of(
                        room("DEMO-20", 20, true, Set.of()),
                        room("DEMO-LAB-24", 24, true, Set.of(RoomFeature.COMPUTER_STATIONS)),
                        room("DEMO-40", 40, true, Set.of(RoomFeature.COMPUTER_STATIONS))
                )
        );

        // Act
        var proposal = planner.propose(scenario);

        // Assert
        assertEquals(2, proposal.assignedGroups());
        assertEquals(0, proposal.unassignedGroups());
        assertEquals(4, proposal.unusedSeats());
        assertEquals(List.of(
                assigned("DEMO-FLEX", "DEMO-20", 2),
                assigned("DEMO-LAB", "DEMO-LAB-24", 2)
        ), proposal.placements());
    }

    @Test
    void rejects_overlapping_room_use_but_allows_adjacent_meetings_to_reuse_the_room() {
        // Arrange
        RoomPlanningScenario scenario = new RoomPlanningScenario(
                List.of(
                        group("DEMO-A", 10, Set.of(), meeting(DayOfWeek.MONDAY, "08:00", "09:00")),
                        group("DEMO-B", 10, Set.of(), meeting(DayOfWeek.MONDAY, "08:30", "09:30")),
                        group("DEMO-C", 10, Set.of(), meeting(DayOfWeek.MONDAY, "09:30", "10:00"))
                ),
                List.of(room("DEMO-ROOM", 12, true, Set.of()))
        );

        // Act
        var proposal = planner.propose(scenario);

        // Assert
        assertEquals(2, proposal.assignedGroups());
        assertEquals(List.of(
                assigned("DEMO-A", "DEMO-ROOM", 2),
                unassigned("DEMO-B", RoomUnassignmentReason.TIME_CONFLICT),
                assigned("DEMO-C", "DEMO-ROOM", 2)
        ), proposal.placements());
    }

    @Test
    void reports_capacity_equipment_and_schedule_conflicts_separately() {
        // Arrange
        RoomPlanningScenario scenario = new RoomPlanningScenario(
                List.of(
                        group("DEMO-CAPACITY", 21, Set.of(), meeting(DayOfWeek.MONDAY, "08:00", "09:00")),
                        group("DEMO-EQUIPMENT", 8, Set.of(RoomFeature.PROJECTOR), meeting(DayOfWeek.MONDAY, "08:00", "09:00")),
                        group("DEMO-CONFLICT-A", 8, Set.of(), meeting(DayOfWeek.MONDAY, "08:00", "09:00")),
                        group("DEMO-CONFLICT-B", 8, Set.of(), meeting(DayOfWeek.MONDAY, "08:00", "09:00"))
                ),
                List.of(
                        room("DEMO-USED", 12, true, Set.of())
                )
        );

        // Act
        var proposal = planner.propose(scenario);

        // Assert
        assertEquals(1, proposal.assignedGroups());
        assertEquals(3, proposal.unassignedGroups());
        assertEquals(List.of(
                unassigned("DEMO-CAPACITY", RoomUnassignmentReason.INSUFFICIENT_CAPACITY),
                assigned("DEMO-CONFLICT-A", "DEMO-USED", 4),
                unassigned("DEMO-CONFLICT-B", RoomUnassignmentReason.TIME_CONFLICT),
                unassigned("DEMO-EQUIPMENT", RoomUnassignmentReason.MISSING_REQUIRED_FEATURES)
        ), proposal.placements());
    }

    @Test
    void reports_when_every_room_is_inactive() {
        // Arrange
        RoomPlanningScenario scenario = new RoomPlanningScenario(
                List.of(group("DEMO-GROUP", 8, Set.of(), meeting(DayOfWeek.MONDAY, "08:00", "09:00"))),
                List.of(room("DEMO-CLOSED", 20, false, Set.of()))
        );

        // Act
        var proposal = planner.propose(scenario);

        // Assert
        assertEquals(0, proposal.assignedGroups());
        assertEquals(unassigned("DEMO-GROUP", RoomUnassignmentReason.NO_ACTIVE_ROOMS), proposal.placements().getFirst());
    }

    @Test
    void produces_the_same_solution_when_input_order_changes() {
        // Arrange
        RoomPlanningScenario original = new RoomPlanningScenario(
                List.of(
                        group("DEMO-B", 14, Set.of(), meeting(DayOfWeek.WEDNESDAY, "08:00", "09:00")),
                        group("DEMO-A", 14, Set.of(), meeting(DayOfWeek.WEDNESDAY, "08:00", "09:00"))
                ),
                List.of(room("DEMO-2", 20, true, Set.of()), room("DEMO-1", 20, true, Set.of()))
        );
        RoomPlanningScenario reordered = new RoomPlanningScenario(
                List.of(original.groups().get(1), original.groups().get(0)),
                List.of(original.rooms().get(1), original.rooms().get(0))
        );

        // Act
        var first = planner.propose(original);
        var second = planner.propose(reordered);

        // Assert
        assertEquals(first, second);
    }

    @Test
    void rejects_duplicate_codes_empty_scenarios_and_invalid_meeting_windows() {
        // Arrange
        RoomResource oneRoom = room("DEMO-ROOM", 20, true, Set.of());
        AcademicGroup oneGroup = group("DEMO-GROUP", 10, Set.of(), meeting(DayOfWeek.MONDAY, "08:00", "09:00"));

        // Act + Assert
        assertThrows(InvalidRoomPlanningScenarioException.class,
                () -> new RoomPlanningScenario(List.of(), List.of(oneRoom)));
        assertThrows(InvalidRoomPlanningScenarioException.class,
                () -> new RoomPlanningScenario(List.of(oneGroup), List.of()));
        assertThrows(InvalidRoomPlanningScenarioException.class,
                () -> new RoomPlanningScenario(List.of(oneGroup, oneGroup), List.of(oneRoom)));
        assertThrows(InvalidRoomPlanningScenarioException.class,
                () -> new RoomPlanningScenario(List.of(oneGroup), List.of(oneRoom, oneRoom)));
        assertThrows(InvalidRoomPlanningScenarioException.class,
                () -> new RoomMeeting(DayOfWeek.MONDAY, LocalTime.of(10, 0), LocalTime.of(10, 0)));
        assertThrows(InvalidRoomPlanningScenarioException.class,
                () -> new RoomMeeting(DayOfWeek.MONDAY, LocalTime.of(11, 0), LocalTime.of(10, 0)));
    }

    @Test
    void rejects_overlapping_meetings_inside_one_group_and_scenarios_over_the_declared_limits() {
        // Arrange
        RoomMeeting first = meeting(DayOfWeek.FRIDAY, "10:00", "11:00");
        RoomMeeting overlap = meeting(DayOfWeek.FRIDAY, "10:30", "11:30");

        // Act + Assert
        assertThrows(InvalidRoomPlanningScenarioException.class,
                () -> new AcademicGroup("DEMO-GROUP", 8, Set.of(), List.of(first, overlap)));
        assertThrows(InvalidRoomPlanningScenarioException.class,
                () -> new AcademicGroup("DEMO-GROUP", 0, Set.of(), List.of(first)));
        assertThrows(InvalidRoomPlanningScenarioException.class,
                () -> new RoomResource("DEMO-ROOM", 0, true, Set.of()));
    }

    @Test
    void fails_closed_when_search_exceeds_its_state_budget() {
        // Arrange
        var boundedPlanner = new DeterministicRoomAssignmentPlanner(1);
        RoomPlanningScenario scenario = new RoomPlanningScenario(
                List.of(
                        group("DEMO-A", 8, Set.of(), meeting(DayOfWeek.MONDAY, "08:00", "09:00")),
                        group("DEMO-B", 8, Set.of(), meeting(DayOfWeek.TUESDAY, "08:00", "09:00"))
                ),
                List.of(room("DEMO-ROOM", 10, true, Set.of()))
        );

        // Act + Assert
        assertThrows(RoomPlanningComplexityException.class, () -> boundedPlanner.propose(scenario));
    }

    private static AcademicGroup group(String code, int seats, Set<RoomFeature> features, RoomMeeting... meetings) {
        return new AcademicGroup(code, seats, features, List.of(meetings));
    }

    private static RoomMeeting meeting(DayOfWeek day, String start, String end) {
        return new RoomMeeting(day, LocalTime.parse(start), LocalTime.parse(end));
    }

    private static RoomResource room(String code, int capacity, boolean active, Set<RoomFeature> features) {
        return new RoomResource(code, capacity, active, features);
    }

    private static RoomPlacement assigned(String group, String room, int remainingSeats) {
        return new RoomPlacement(group, RoomAssignmentStatus.ASSIGNED, room, remainingSeats, null);
    }

    private static RoomPlacement unassigned(String group, RoomUnassignmentReason reason) {
        return new RoomPlacement(group, RoomAssignmentStatus.UNASSIGNED, null, null, reason);
    }
}
