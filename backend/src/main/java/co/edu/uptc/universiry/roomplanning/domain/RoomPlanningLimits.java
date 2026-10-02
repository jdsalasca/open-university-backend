package co.edu.uptc.universiry.roomplanning.domain;

public final class RoomPlanningLimits {

    public static final int MAX_GROUPS = 12;
    public static final int MAX_ROOMS = 24;
    public static final int MAX_MEETINGS_PER_GROUP = 8;
    public static final int MAX_PARTICIPANTS = 2_000;
    public static final int MAX_CODE_LENGTH = 32;
    public static final int MAX_SEARCH_STATES = 100_000;

    private RoomPlanningLimits() {
    }
}
