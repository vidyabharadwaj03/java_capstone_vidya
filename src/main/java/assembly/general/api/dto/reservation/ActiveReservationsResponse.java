package assembly.general.api.dto.reservation;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
@AllArgsConstructor
public class ActiveReservationsResponse {
    private List<ActiveReservationItem> reservations;
    private long totalActive;
}
