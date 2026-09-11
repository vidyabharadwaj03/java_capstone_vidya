package assembly.general.api.exception;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    @Test
    void duplicateEmailMapsToBadRequestWithValidationError() {
        ResponseEntity<Map<String, Object>> response =
                handler.handleDuplicateEmail(new DuplicateEmailException("Email already exists"));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).containsEntry("error", "VALIDATION_ERROR");
        assertThat(response.getBody()).containsEntry("message", "Email already exists");
    }

    @Test
    void invalidCredentialsMapsToUnauthorized() {
        ResponseEntity<Map<String, Object>> response =
                handler.handleInvalidCredentials(new InvalidCredentialsException("Invalid email or password"));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(response.getBody()).containsEntry("error", "AUTHENTICATION_FAILED");
    }

    @Test
    void resourceNotFoundMapsToNotFound() {
        ResponseEntity<Map<String, Object>> response =
                handler.handleResourceNotFound(new ResourceNotFoundException("Book not found with ID: 123"));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(response.getBody()).containsEntry("error", "NOT_FOUND");
    }

    @Test
    void reservationLimitExceededIncludesCurrentCount() {
        ResponseEntity<Map<String, Object>> response = handler.handleReservationLimitExceeded(
                new ReservationLimitExceededException("You have reached the maximum of 5 active reservations", 5));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).containsEntry("error", "RESERVATION_LIMIT_EXCEEDED");
        assertThat(response.getBody()).containsEntry("currentReservations", 5);
    }

    @Test
    void bookUnavailableIncludesAvailableCopies() {
        ResponseEntity<Map<String, Object>> response = handler.handleBookUnavailable(
                new BookUnavailableException("No copies available for reservation", 0));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).containsEntry("error", "BOOK_UNAVAILABLE");
        assertThat(response.getBody()).containsEntry("availableCopies", 0);
    }

    @Test
    void invalidStatusIncludesCurrentStatus() {
        ResponseEntity<Map<String, Object>> response = handler.handleInvalidStatus(
                new InvalidReservationStatusException("Can only checkout reservations with RESERVED status", "CHECKED_OUT"));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).containsEntry("error", "INVALID_STATUS");
        assertThat(response.getBody()).containsEntry("currentStatus", "CHECKED_OUT");
    }

    @Test
    void forbiddenOperationMapsToForbidden() {
        ResponseEntity<Map<String, Object>> response =
                handler.handleForbidden(new ForbiddenOperationException("Only librarians can checkout books"));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(response.getBody()).containsEntry("error", "FORBIDDEN");
    }

    @Test
    void genericExceptionMapsToInternalServerError() {
        ResponseEntity<Map<String, Object>> response = handler.handleGeneric(new RuntimeException("boom"));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        assertThat(response.getBody()).containsEntry("error", "INTERNAL_SERVER_ERROR");
    }
}
