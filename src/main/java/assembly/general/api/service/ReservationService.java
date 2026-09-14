package assembly.general.api.service;

import assembly.general.api.dto.PagedResponse;
import assembly.general.api.dto.reservation.ActiveReservationItem;
import assembly.general.api.dto.reservation.ActiveReservationsResponse;
import assembly.general.api.dto.reservation.CheckoutRequest;
import assembly.general.api.dto.reservation.CheckoutResponse;
import assembly.general.api.dto.reservation.HistoryItem;
import assembly.general.api.dto.reservation.ReservationResponse;
import assembly.general.api.dto.reservation.ReturnRequest;
import assembly.general.api.dto.reservation.ReturnResponse;
import assembly.general.api.entity.Book;
import assembly.general.api.entity.Reservation;
import assembly.general.api.entity.ReservationStatus;
import assembly.general.api.entity.Role;
import assembly.general.api.entity.User;
import assembly.general.api.exception.BookUnavailableException;
import assembly.general.api.exception.ForbiddenOperationException;
import assembly.general.api.exception.InvalidReservationStatusException;
import assembly.general.api.exception.ReservationLimitExceededException;
import assembly.general.api.exception.ResourceNotFoundException;
import assembly.general.api.repository.BookRepository;
import assembly.general.api.repository.ReservationRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;

@Service
public class ReservationService {

    private static final int MAX_ACTIVE_RESERVATIONS = 5;
    private static final int RESERVATION_EXPIRY_DAYS = 7;
    private static final int CHECKOUT_PERIOD_DAYS = 14;
    private static final BigDecimal LATE_FEE_PER_DAY = BigDecimal.ONE;
    private static final DateTimeFormatter DUE_DATE_FORMAT =
            DateTimeFormatter.ofPattern("MMMM d, yyyy").withZone(ZoneOffset.UTC);

    private final ReservationRepository reservationRepository;
    private final BookRepository bookRepository;
    private final Clock clock;

    public ReservationService(ReservationRepository reservationRepository, BookRepository bookRepository, Clock clock) {
        this.reservationRepository = reservationRepository;
        this.bookRepository = bookRepository;
        this.clock = clock;
    }

    @Transactional
    public ReservationResponse createReservation(User user, UUID bookId) {
        long activeCount = reservationRepository.countByUserIdAndStatusIn(
                user.getId(), List.of(ReservationStatus.RESERVED, ReservationStatus.CHECKED_OUT));

        if (activeCount >= MAX_ACTIVE_RESERVATIONS) {
            throw new ReservationLimitExceededException(
                    "You have reached the maximum of 5 active reservations", (int) activeCount);
        }

        Book book = bookRepository.findByIdForUpdate(bookId)
                .orElseThrow(() -> new ResourceNotFoundException("Book not found with ID: " + bookId));

        if (book.getAvailableCopies() <= 0) {
            throw new BookUnavailableException("No copies available for reservation", book.getAvailableCopies());
        }

        Instant now = Instant.now(clock);

        Reservation reservation = Reservation.builder()
                .book(book)
                .user(user)
                .status(ReservationStatus.RESERVED)
                .reservedAt(now)
                .expiresAt(now.plus(RESERVATION_EXPIRY_DAYS, ChronoUnit.DAYS))
                .build();

        book.setAvailableCopies(book.getAvailableCopies() - 1);
        bookRepository.save(book);
        Reservation saved = reservationRepository.save(reservation);

        return ReservationResponse.builder()
                .reservationId(saved.getId())
                .bookId(book.getId())
                .userId(user.getId())
                .bookTitle(book.getTitle())
                .status(saved.getStatus())
                .reservedAt(saved.getReservedAt())
                .expiresAt(saved.getExpiresAt())
                .message("Book reserved successfully. Please pick up within 7 days.")
                .build();
    }

    public ActiveReservationsResponse getActiveReservations(User user) {
        List<Reservation> reservations = reservationRepository.findByUserIdAndStatusInOrderByReservedAtDesc(
                user.getId(), List.of(ReservationStatus.RESERVED, ReservationStatus.CHECKED_OUT));

        Instant now = Instant.now(clock);

        List<ActiveReservationItem> items = reservations.stream()
                .map(reservation -> toActiveItem(reservation, now))
                .toList();

        return ActiveReservationsResponse.builder()
                .reservations(items)
                .totalActive(items.size())
                .build();
    }

    @Transactional
    public CheckoutResponse checkout(User librarian, UUID reservationId, CheckoutRequest request) {
        requireLibrarian(librarian, "Only librarians can checkout books");

        Reservation reservation = reservationRepository.findById(reservationId)
                .orElseThrow(() -> new ResourceNotFoundException("Reservation not found with ID: " + reservationId));

        if (reservation.getStatus() != ReservationStatus.RESERVED) {
            throw new InvalidReservationStatusException(
                    "Can only checkout reservations with RESERVED status", reservation.getStatus().name());
        }

        Instant now = Instant.now(clock);
        Instant dueDate = now.plus(CHECKOUT_PERIOD_DAYS, ChronoUnit.DAYS);

        reservation.setStatus(ReservationStatus.CHECKED_OUT);
        reservation.setCheckedOutAt(now);
        reservation.setDueDate(dueDate);
        if (request != null) {
            reservation.setNotes(request.getNotes());
        }

        reservationRepository.save(reservation);

        return CheckoutResponse.builder()
                .reservationId(reservation.getId())
                .status(reservation.getStatus())
                .checkedOutAt(now)
                .dueDate(dueDate)
                .message("Book checked out successfully. Due date: " + DUE_DATE_FORMAT.format(dueDate))
                .build();
    }

    @Transactional
    public ReturnResponse returnBook(User librarian, UUID reservationId, ReturnRequest request) {
        requireLibrarian(librarian, "Only librarians can process returns");

        Reservation reservation = reservationRepository.findById(reservationId)
                .orElseThrow(() -> new ResourceNotFoundException("Reservation not found with ID: " + reservationId));

        if (reservation.getStatus() != ReservationStatus.CHECKED_OUT) {
            throw new InvalidReservationStatusException(
                    "Can only return books with CHECKED_OUT status", reservation.getStatus().name());
        }

        Instant now = Instant.now(clock);
        int lateDays = calculateLateDays(reservation.getDueDate(), now);
        BigDecimal lateFee = LATE_FEE_PER_DAY.multiply(BigDecimal.valueOf(lateDays));

        reservation.setStatus(ReservationStatus.RETURNED);
        reservation.setReturnedAt(now);
        reservation.setCondition(request.getCondition());
        reservation.setNotes(request.getNotes());
        reservation.setLateDays(lateDays);
        reservation.setLateFee(lateFee);

        Book book = bookRepository.findByIdForUpdate(reservation.getBook().getId())
                .orElseThrow(() -> new ResourceNotFoundException("Book not found with ID: " + reservation.getBook().getId()));
        book.setAvailableCopies(book.getAvailableCopies() + 1);
        bookRepository.save(book);
        reservationRepository.save(reservation);

        String message = lateDays > 0
                ? String.format("Book returned. Late fee of $%.2f applied to account.", lateFee)
                : "Book returned successfully";

        return ReturnResponse.builder()
                .reservationId(reservation.getId())
                .returnedAt(now)
                .dueDate(reservation.getDueDate())
                .lateDays(lateDays)
                .lateFee(lateFee)
                .message(message)
                .build();
    }

    public PagedResponse<HistoryItem> getHistory(User user, int page, int size) {
        Page<Reservation> reservations = reservationRepository.findHistoryByUserId(
                user.getId(), PageRequest.of(page, size));

        Page<HistoryItem> mapped = reservations.map(this::toHistoryItem);
        return PagedResponse.from(mapped);
    }

    private void requireLibrarian(User user, String message) {
        if (user.getRole() != Role.LIBRARIAN) {
            throw new ForbiddenOperationException(message);
        }
    }

    private int calculateLateDays(Instant dueDate, Instant returnedAt) {
        if (dueDate == null || !returnedAt.isAfter(dueDate)) {
            return 0;
        }
        LocalDate dueLocalDate = dueDate.atZone(ZoneOffset.UTC).toLocalDate();
        LocalDate returnedLocalDate = returnedAt.atZone(ZoneOffset.UTC).toLocalDate();
        return (int) ChronoUnit.DAYS.between(dueLocalDate, returnedLocalDate);
    }

    private ActiveReservationItem toActiveItem(Reservation reservation, Instant now) {
        ActiveReservationItem.ActiveReservationItemBuilder builder = ActiveReservationItem.builder()
                .reservationId(reservation.getId())
                .bookId(reservation.getBook().getId())
                .bookTitle(reservation.getBook().getTitle())
                .bookAuthor(reservation.getBook().getAuthor())
                .status(reservation.getStatus());

        if (reservation.getStatus() == ReservationStatus.RESERVED) {
            builder.reservedAt(reservation.getReservedAt())
                    .expiresAt(reservation.getExpiresAt())
                    .daysUntilExpiry(ChronoUnit.DAYS.between(now, reservation.getExpiresAt()));
        } else {
            builder.checkedOutAt(reservation.getCheckedOutAt())
                    .dueDate(reservation.getDueDate())
                    .daysUntilDue(ChronoUnit.DAYS.between(now, reservation.getDueDate()));
        }

        return builder.build();
    }

    private HistoryItem toHistoryItem(Reservation reservation) {
        boolean wasLate = reservation.getReturnedAt() != null
                && reservation.getDueDate() != null
                && reservation.getReturnedAt().isAfter(reservation.getDueDate());

        return HistoryItem.builder()
                .reservationId(reservation.getId())
                .bookTitle(reservation.getBook().getTitle())
                .bookAuthor(reservation.getBook().getAuthor())
                .reservedAt(reservation.getReservedAt())
                .checkedOutAt(reservation.getCheckedOutAt())
                .returnedAt(reservation.getReturnedAt())
                .dueDate(reservation.getDueDate())
                .status(reservation.getStatus())
                .wasLate(wasLate)
                .build();
    }
}
