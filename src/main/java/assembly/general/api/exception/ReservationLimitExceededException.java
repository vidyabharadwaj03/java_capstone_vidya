package assembly.general.api.exception;

import lombok.Getter;

@Getter
public class ReservationLimitExceededException extends RuntimeException {

    private final int currentReservations;

    public ReservationLimitExceededException(String message, int currentReservations) {
        super(message);
        this.currentReservations = currentReservations;
    }
}
