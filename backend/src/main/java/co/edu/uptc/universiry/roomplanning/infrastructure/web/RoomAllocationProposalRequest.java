package co.edu.uptc.universiry.roomplanning.infrastructure.web;

import co.edu.uptc.universiry.roomplanning.domain.AcademicGroup;
import co.edu.uptc.universiry.roomplanning.domain.RoomFeature;
import co.edu.uptc.universiry.roomplanning.domain.RoomMeeting;
import co.edu.uptc.universiry.roomplanning.domain.RoomPlanningLimits;
import co.edu.uptc.universiry.roomplanning.domain.RoomPlanningScenario;
import co.edu.uptc.universiry.roomplanning.domain.RoomResource;
import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.List;
import java.util.Set;

public record RoomAllocationProposalRequest(
        @NotEmpty @Size(max = RoomPlanningLimits.MAX_GROUPS)
        List<@NotNull @Valid GroupRequest> groups,
        @NotEmpty @Size(max = RoomPlanningLimits.MAX_ROOMS)
        List<@NotNull @Valid RoomRequest> rooms
) {

    public RoomPlanningScenario toScenario() {
        return new RoomPlanningScenario(
                groups.stream()
                        .map(group -> new AcademicGroup(
                                group.code(), group.expectedEnrollment(), group.requiredFeatures(),
                                group.meetings().stream()
                                        .map(meeting -> new RoomMeeting(meeting.day(), meeting.startsAt(), meeting.endsAt()))
                                        .toList()))
                        .toList(),
                rooms.stream()
                        .map(room -> new RoomResource(room.code(), room.capacity(), room.active(), room.features()))
                        .toList()
        );
    }

    public record GroupRequest(
            @NotBlank @Size(max = RoomPlanningLimits.MAX_CODE_LENGTH) String code,
            @Min(1) @Max(RoomPlanningLimits.MAX_PARTICIPANTS) int expectedEnrollment,
            @NotNull Set<@NotNull RoomFeature> requiredFeatures,
            @NotEmpty @Size(max = RoomPlanningLimits.MAX_MEETINGS_PER_GROUP)
            List<@NotNull @Valid MeetingRequest> meetings
    ) {
    }

    public record RoomRequest(
            @NotBlank @Size(max = RoomPlanningLimits.MAX_CODE_LENGTH) String code,
            @Min(1) @Max(RoomPlanningLimits.MAX_PARTICIPANTS) int capacity,
            @NotNull Boolean active,
            @NotNull Set<@NotNull RoomFeature> features
    ) {
    }

    public record MeetingRequest(
            @NotNull DayOfWeek day,
            @NotNull @JsonFormat(pattern = "HH:mm") LocalTime startsAt,
            @NotNull @JsonFormat(pattern = "HH:mm") LocalTime endsAt
    ) {
    }
}
