package assembly.general.api.dto.reservation;

import assembly.general.api.entity.ReservationStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.time.Instant;
import java.util.UUID;

@Getter
@Builder
@AllArgsConstructor
public class CheckoutResponse {
    private UUID reservationId;
    private ReservationStatus status;
    private Instant checkedOutAt;
    private Instant dueDate;
    private String message;
}
