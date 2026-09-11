package assembly.general.api.dto.reservation;

import assembly.general.api.entity.ReservationStatus;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.time.Instant;
import java.util.UUID;

@Getter
@Builder
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ActiveReservationItem {
    private UUID reservationId;
    private UUID bookId;
    private String bookTitle;
    private String bookAuthor;
    private ReservationStatus status;
    private Instant reservedAt;
    private Instant expiresAt;
    private Long daysUntilExpiry;
    private Instant checkedOutAt;
    private Instant dueDate;
    private Long daysUntilDue;
}
