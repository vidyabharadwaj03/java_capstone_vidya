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
public class HistoryItem {
    private UUID reservationId;
    private String bookTitle;
    private String bookAuthor;
    private Instant reservedAt;
    private Instant checkedOutAt;
    private Instant returnedAt;
    private Instant dueDate;
    private ReservationStatus status;
    private boolean wasLate;
}
