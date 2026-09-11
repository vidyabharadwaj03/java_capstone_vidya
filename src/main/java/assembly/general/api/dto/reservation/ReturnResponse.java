package assembly.general.api.dto.reservation;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Getter
@Builder
@AllArgsConstructor
public class ReturnResponse {
    private UUID reservationId;
    private Instant returnedAt;
    private Instant dueDate;
    private int lateDays;
    private BigDecimal lateFee;
    private String message;
}
