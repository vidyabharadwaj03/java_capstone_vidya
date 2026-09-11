package assembly.general.api.exception;

import lombok.Getter;

@Getter
public class InvalidReservationStatusException extends RuntimeException {

    private final String currentStatus;

    public InvalidReservationStatusException(String message, String currentStatus) {
        super(message);
        this.currentStatus = currentStatus;
    }
}
