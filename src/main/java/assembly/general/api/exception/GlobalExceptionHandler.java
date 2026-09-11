package assembly.general.api.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(DuplicateEmailException.class)
    public ResponseEntity<Map<String, Object>> handleDuplicateEmail(DuplicateEmailException ex) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(errorBody("VALIDATION_ERROR", ex.getMessage()));
    }

    @ExceptionHandler(InvalidCredentialsException.class)
    public ResponseEntity<Map<String, Object>> handleInvalidCredentials(InvalidCredentialsException ex) {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(errorBody("AUTHENTICATION_FAILED", ex.getMessage()));
    }

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<Map<String, Object>> handleResourceNotFound(ResourceNotFoundException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(errorBody("NOT_FOUND", ex.getMessage()));
    }

    @ExceptionHandler(ReservationLimitExceededException.class)
    public ResponseEntity<Map<String, Object>> handleReservationLimitExceeded(ReservationLimitExceededException ex) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("error", "RESERVATION_LIMIT_EXCEEDED");
        body.put("message", ex.getMessage());
        body.put("currentReservations", ex.getCurrentReservations());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(body);
    }

    @ExceptionHandler(BookUnavailableException.class)
    public ResponseEntity<Map<String, Object>> handleBookUnavailable(BookUnavailableException ex) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("error", "BOOK_UNAVAILABLE");
        body.put("message", ex.getMessage());
        body.put("availableCopies", ex.getAvailableCopies());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(body);
    }

    @ExceptionHandler(InvalidReservationStatusException.class)
    public ResponseEntity<Map<String, Object>> handleInvalidStatus(InvalidReservationStatusException ex) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("error", "INVALID_STATUS");
        body.put("message", ex.getMessage());
        body.put("currentStatus", ex.getCurrentStatus());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(body);
    }

    @ExceptionHandler(ForbiddenOperationException.class)
    public ResponseEntity<Map<String, Object>> handleForbidden(ForbiddenOperationException ex) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(errorBody("FORBIDDEN", ex.getMessage()));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, Object>> handleValidation(MethodArgumentNotValidException ex) {
        String message = ex.getBindingResult().getFieldErrors().stream()
                .findFirst()
                .map(FieldError::getDefaultMessage)
                .orElse("Validation failed");
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(errorBody("VALIDATION_ERROR", message));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, Object>> handleGeneric(Exception ex) {
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(errorBody("INTERNAL_SERVER_ERROR", "An unexpected error occurred"));
    }

    private Map<String, Object> errorBody(String error, String message) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("error", error);
        body.put("message", message);
        body.put("timestamp", Instant.now().toString());
        return body;
    }
}
