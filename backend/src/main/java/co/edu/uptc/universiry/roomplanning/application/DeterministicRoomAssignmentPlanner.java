package co.edu.uptc.universiry.roomplanning.application;

import co.edu.uptc.universiry.roomplanning.domain.AcademicGroup;
import co.edu.uptc.universiry.roomplanning.domain.RoomAssignmentStatus;
import co.edu.uptc.universiry.roomplanning.domain.RoomFeature;
import co.edu.uptc.universiry.roomplanning.domain.RoomMeeting;
import co.edu.uptc.universiry.roomplanning.domain.RoomPlacement;
import co.edu.uptc.universiry.roomplanning.domain.RoomPlanningLimits;
import co.edu.uptc.universiry.roomplanning.domain.RoomPlanningProposal;
import co.edu.uptc.universiry.roomplanning.domain.RoomPlanningScenario;
import co.edu.uptc.universiry.roomplanning.domain.RoomResource;
import co.edu.uptc.universiry.roomplanning.domain.RoomUnassignmentReason;
import org.springframework.stereotype.Component;

import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
public final class DeterministicRoomAssignmentPlanner implements RoomAssignmentPlanner {

    private final int maximumSearchStates;

    public DeterministicRoomAssignmentPlanner() {
        this(RoomPlanningLimits.MAX_SEARCH_STATES);
    }

    DeterministicRoomAssignmentPlanner(int maximumSearchStates) {
        if (maximumSearchStates < 1) throw new IllegalArgumentException("Search state budget must be positive.");
        this.maximumSearchStates = maximumSearchStates;
    }

    @Override
    public RoomPlanningProposal propose(RoomPlanningScenario scenario) {
        List<RoomResource> activeRooms = scenario.rooms().stream()
                .filter(RoomResource::active)
                .sorted(Comparator.comparing(RoomResource::code))
                .toList();
        Map<String, List<RoomResource>> candidatesByGroup = new HashMap<>();
        Map<String, RoomUnassignmentReason> staticReasons = new HashMap<>();

        for (AcademicGroup group : scenario.groups()) {
            List<RoomResource> byCapacity = activeRooms.stream()
                    .filter(room -> room.capacity() >= group.expectedEnrollment())
                    .toList();
            List<RoomResource> compatible = byCapacity.stream()
                    .filter(room -> room.features().containsAll(group.requiredFeatures()))
                    .sorted(Comparator
                            .comparingInt((RoomResource room) -> room.capacity() - group.expectedEnrollment())
                            .thenComparing(RoomResource::code))
                    .toList();
            if (!compatible.isEmpty()) {
                candidatesByGroup.put(group.code(), compatible);
            } else if (activeRooms.isEmpty()) {
                staticReasons.put(group.code(), RoomUnassignmentReason.NO_ACTIVE_ROOMS);
            } else if (byCapacity.isEmpty()) {
                staticReasons.put(group.code(), RoomUnassignmentReason.INSUFFICIENT_CAPACITY);
            } else {
                staticReasons.put(group.code(), RoomUnassignmentReason.MISSING_REQUIRED_FEATURES);
            }
        }

        List<AcademicGroup> constrainedGroups = scenario.groups().stream()
                .filter(group -> candidatesByGroup.containsKey(group.code()))
                .sorted(Comparator
                        .comparingInt((AcademicGroup group) -> candidatesByGroup.get(group.code()).size())
                        .thenComparing(Comparator.comparingInt((AcademicGroup group) -> group.requiredFeatures().size()).reversed())
                        .thenComparing(Comparator.comparingInt(AcademicGroup::expectedEnrollment).reversed())
                        .thenComparing(AcademicGroup::code))
                .toList();

        Search search = new Search(constrainedGroups, candidatesByGroup, maximumSearchStates);
        Map<String, RoomResource> assignments = search.solve();

        List<RoomPlacement> placements = scenario.groups().stream()
                .sorted(Comparator.comparing(AcademicGroup::code))
                .map(group -> placement(group, assignments.get(group.code()), staticReasons.get(group.code())))
                .toList();
        int assigned = assignments.size();
        int unassigned = placements.size() - assigned;
        int unusedSeats = placements.stream()
                .filter(placement -> placement.status() == RoomAssignmentStatus.ASSIGNED)
                .mapToInt(RoomPlacement::remainingSeats)
                .sum();
        return new RoomPlanningProposal(placements, assigned, unassigned, unusedSeats);
    }

    private static RoomPlacement placement(
            AcademicGroup group,
            RoomResource room,
            RoomUnassignmentReason staticReason
    ) {
        if (room != null) {
            return new RoomPlacement(group.code(), RoomAssignmentStatus.ASSIGNED, room.code(),
                    room.capacity() - group.expectedEnrollment(), null);
        }
        RoomUnassignmentReason reason = staticReason == null
                ? RoomUnassignmentReason.TIME_CONFLICT
                : staticReason;
        return new RoomPlacement(group.code(), RoomAssignmentStatus.UNASSIGNED, null, null, reason);
    }

    private static final class Search {

        private final List<AcademicGroup> groups;
        private final Map<String, List<RoomResource>> candidatesByGroup;
        private final int maximumStates;
        private final Map<String, RoomResource> current = new HashMap<>();
        private int visitedStates;
        private int bestAssigned = -1;
        private int bestUnusedSeats = Integer.MAX_VALUE;
        private Map<String, RoomResource> best = Map.of();

        private Search(List<AcademicGroup> groups, Map<String, List<RoomResource>> candidatesByGroup, int maximumStates) {
            this.groups = groups;
            this.candidatesByGroup = candidatesByGroup;
            this.maximumStates = maximumStates;
        }

        private Map<String, RoomResource> solve() {
            visit(0, 0);
            return best;
        }

        private void visit(int index, int unusedSeats) {
            visitedStates++;
            if (visitedStates > maximumStates) throw new RoomPlanningComplexityException();
            if (cannotImprove(index, unusedSeats)) return;
            if (index == groups.size()) {
                rememberIfBetter(unusedSeats);
                return;
            }

            AcademicGroup group = groups.get(index);
            for (RoomResource room : candidatesByGroup.get(group.code())) {
                if (canUse(room, group)) {
                    current.put(group.code(), room);
                    visit(index + 1, unusedSeats + room.capacity() - group.expectedEnrollment());
                    current.remove(group.code());
                }
            }
            visit(index + 1, unusedSeats);
        }

        private boolean cannotImprove(int index, int unusedSeats) {
            int maximumPlaced = current.size() + groups.size() - index;
            if (maximumPlaced < bestAssigned) return true;
            if (bestAssigned < 0 || maximumPlaced != bestAssigned) return false;

            int minimumRemainingSeats = 0;
            for (int remaining = index; remaining < groups.size(); remaining++) {
                AcademicGroup group = groups.get(remaining);
                minimumRemainingSeats += candidatesByGroup.get(group.code()).stream()
                        .mapToInt(room -> room.capacity() - group.expectedEnrollment())
                        .min()
                        .orElse(0);
            }
            // Search order defines the stable tie-break, so an equal lower bound cannot improve the first best result.
            return unusedSeats + minimumRemainingSeats >= bestUnusedSeats;
        }

        private void rememberIfBetter(int unusedSeats) {
            int assigned = current.size();
            if (assigned > bestAssigned || (assigned == bestAssigned && unusedSeats < bestUnusedSeats)) {
                bestAssigned = assigned;
                bestUnusedSeats = unusedSeats;
                best = Map.copyOf(current);
            }
        }

        private boolean canUse(RoomResource room, AcademicGroup candidate) {
            for (Map.Entry<String, RoomResource> assignment : current.entrySet()) {
                if (assignment.getValue().code().equals(room.code())) {
                    AcademicGroup other = groupByCode(assignment.getKey());
                    if (hasOverlappingMeetings(candidate, other)) return false;
                }
            }
            return true;
        }

        private AcademicGroup groupByCode(String code) {
            return groups.stream().filter(group -> group.code().equals(code)).findFirst().orElseThrow();
        }

        private static boolean hasOverlappingMeetings(AcademicGroup first, AcademicGroup second) {
            for (RoomMeeting firstMeeting : first.meetings()) {
                for (RoomMeeting secondMeeting : second.meetings()) {
                    if (firstMeeting.overlaps(secondMeeting)) return true;
                }
            }
            return false;
        }
    }
}
