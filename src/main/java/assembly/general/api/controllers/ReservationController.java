package assembly.general.api.controllers;

import assembly.general.api.dto.PagedResponse;
import assembly.general.api.dto.reservation.ActiveReservationsResponse;
import assembly.general.api.dto.reservation.CheckoutRequest;
import assembly.general.api.dto.reservation.CheckoutResponse;
import assembly.general.api.dto.reservation.CreateReservationRequest;
import assembly.general.api.dto.reservation.HistoryItem;
import assembly.general.api.dto.reservation.ReservationResponse;
import assembly.general.api.dto.reservation.ReturnRequest;
import assembly.general.api.dto.reservation.ReturnResponse;
import assembly.general.api.entity.User;
import assembly.general.api.service.ReservationService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/reservations")
public class ReservationController {

    private final ReservationService reservationService;

    public ReservationController(ReservationService reservationService) {
        this.reservationService = reservationService;
    }

    @PostMapping
    public ResponseEntity<ReservationResponse> createReservation(@AuthenticationPrincipal User user,
                                                                   @Valid @RequestBody CreateReservationRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(reservationService.createReservation(user, request.getBookId()));
    }

    @GetMapping
    public ResponseEntity<ActiveReservationsResponse> getActiveReservations(@AuthenticationPrincipal User user) {
        return ResponseEntity.ok(reservationService.getActiveReservations(user));
    }

    @PostMapping("/{reservationId}/checkout")
    public ResponseEntity<CheckoutResponse> checkout(@AuthenticationPrincipal User user,
                                                       @PathVariable("reservationId") UUID reservationId,
                                                       @RequestBody(required = false) CheckoutRequest request) {
        return ResponseEntity.ok(reservationService.checkout(user, reservationId, request));
    }

    @PostMapping("/{reservationId}/return")
    public ResponseEntity<ReturnResponse> returnBook(@AuthenticationPrincipal User user,
                                                       @PathVariable("reservationId") UUID reservationId,
                                                       @Valid @RequestBody ReturnRequest request) {
        return ResponseEntity.ok(reservationService.returnBook(user, reservationId, request));
    }

    @GetMapping("/history")
    public ResponseEntity<PagedResponse<HistoryItem>> getHistory(@AuthenticationPrincipal User user,
                                                                   @RequestParam(defaultValue = "0") int page,
                                                                   @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(reservationService.getHistory(user, page, size));
    }
}
