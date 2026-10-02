package co.edu.uptc.universiry.roomplanning.domain;

import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.Objects;

public record RoomMeeting(DayOfWeek day, LocalTime startsAt, LocalTime endsAt) {

    public RoomMeeting {
        Objects.requireNonNull(day, "Meeting day is required.");
        Objects.requireNonNull(startsAt, "Meeting start is required.");
        Objects.requireNonNull(endsAt, "Meeting end is required.");
        if (!startsAt.isBefore(endsAt)) {
            throw new InvalidRoomPlanningScenarioException("A meeting must end after it starts.");
        }
    }

    public boolean overlaps(RoomMeeting other) {
        return day == other.day
                && startsAt.isBefore(other.endsAt)
                && other.startsAt.isBefore(endsAt);
    }
}
