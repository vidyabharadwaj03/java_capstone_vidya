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
public class ReservationResponse {
    private UUID reservationId;
    private UUID bookId;
    private UUID userId;
    private String bookTitle;
    private ReservationStatus status;
    private Instant reservedAt;
    private Instant expiresAt;
    private String message;
}
