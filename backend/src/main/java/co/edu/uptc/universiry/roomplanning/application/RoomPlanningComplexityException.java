package co.edu.uptc.universiry.roomplanning.application;

public final class RoomPlanningComplexityException extends RuntimeException {

    public RoomPlanningComplexityException() {
        super("Room planning exceeded its search state budget.");
    }
}
