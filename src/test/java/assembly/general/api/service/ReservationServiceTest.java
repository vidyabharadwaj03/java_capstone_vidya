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
import assembly.general.api.entity.BookCondition;
import assembly.general.api.entity.MembershipStatus;
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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ReservationServiceTest {

    @Mock
    private ReservationRepository reservationRepository;

    @Mock
    private BookRepository bookRepository;

    private ReservationService reservationService;
    private Clock clock;

    private User patron;
    private User librarian;
    private Book book;

    @BeforeEach
    void setUp() {
        clock = Clock.fixed(Instant.parse("2025-09-01T00:00:00Z"), ZoneOffset.UTC);
        reservationService = new ReservationService(reservationRepository, bookRepository, clock);

        patron = User.builder()
                .id(UUID.randomUUID())
                .email("patron@example.com")
                .role(Role.PATRON)
                .membershipStatus(MembershipStatus.ACTIVE)
                .memberSince(Instant.now())
                .build();

        librarian = User.builder()
                .id(UUID.randomUUID())
                .email("librarian@example.com")
                .role(Role.LIBRARIAN)
                .membershipStatus(MembershipStatus.ACTIVE)
                .memberSince(Instant.now())
                .build();

        book = Book.builder()
                .id(UUID.randomUUID())
                .isbn("111")
                .title("Clean Code")
                .author("Robert Martin")
                .genre("Technology")
                .publicationYear(2008)
                .totalCopies(5)
                .availableCopies(2)
                .build();
    }

    @Test
    void createReservationDecrementsAvailableCopiesAndSetsExpiry() {
        when(reservationRepository.countByUserIdAndStatusIn(eq(patron.getId()), any())).thenReturn(0L);
        when(bookRepository.findByIdForUpdate(book.getId())).thenReturn(Optional.of(book));
        when(reservationRepository.save(any(Reservation.class))).thenAnswer(invocation -> invocation.getArgument(0));

        ReservationResponse response = reservationService.createReservation(patron, book.getId());

        assertThat(response.getStatus()).isEqualTo(ReservationStatus.RESERVED);
        assertThat(response.getExpiresAt()).isEqualTo(response.getReservedAt().plus(7, ChronoUnit.DAYS));
        assertThat(book.getAvailableCopies()).isEqualTo(1);
        verify(bookRepository).save(book);
    }

    @Test
    void createReservationThrowsWhenLimitReached() {
        when(reservationRepository.countByUserIdAndStatusIn(eq(patron.getId()), any())).thenReturn(5L);

        assertThatThrownBy(() -> reservationService.createReservation(patron, book.getId()))
                .isInstanceOf(ReservationLimitExceededException.class);

        verify(bookRepository, never()).findByIdForUpdate(any());
    }

    @Test
    void createReservationThrowsWhenBookMissing() {
        when(reservationRepository.countByUserIdAndStatusIn(eq(patron.getId()), any())).thenReturn(0L);
        when(bookRepository.findByIdForUpdate(book.getId())).thenReturn(Optional.empty());

        assertThatThrownBy(() -> reservationService.createReservation(patron, book.getId()))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void createReservationThrowsWhenNoCopiesAvailable() {
        book.setAvailableCopies(0);
        when(reservationRepository.countByUserIdAndStatusIn(eq(patron.getId()), any())).thenReturn(0L);
        when(bookRepository.findByIdForUpdate(book.getId())).thenReturn(Optional.of(book));

        assertThatThrownBy(() -> reservationService.createReservation(patron, book.getId()))
                .isInstanceOf(BookUnavailableException.class);

        verify(reservationRepository, never()).save(any());
    }

    @Test
    void checkoutRejectsNonLibrarian() {
        Reservation reservation = Reservation.builder()
                .id(UUID.randomUUID())
                .book(book)
                .user(patron)
                .status(ReservationStatus.RESERVED)
                .build();

        assertThatThrownBy(() -> reservationService.checkout(patron, reservation.getId(), new CheckoutRequest()))
                .isInstanceOf(ForbiddenOperationException.class);
    }

    @Test
    void checkoutRejectsNonReservedStatus() {
        UUID reservationId = UUID.randomUUID();
        Reservation reservation = Reservation.builder()
                .id(reservationId)
                .book(book)
                .user(patron)
                .status(ReservationStatus.CHECKED_OUT)
                .build();
        when(reservationRepository.findById(reservationId)).thenReturn(Optional.of(reservation));

        assertThatThrownBy(() -> reservationService.checkout(librarian, reservationId, new CheckoutRequest()))
                .isInstanceOf(InvalidReservationStatusException.class);
    }

    @Test
    void checkoutSetsCheckedOutStatusAndDueDate() {
        UUID reservationId = UUID.randomUUID();
        Reservation reservation = Reservation.builder()
                .id(reservationId)
                .book(book)
                .user(patron)
                .status(ReservationStatus.RESERVED)
                .build();
        when(reservationRepository.findById(reservationId)).thenReturn(Optional.of(reservation));
        when(reservationRepository.save(any(Reservation.class))).thenAnswer(invocation -> invocation.getArgument(0));

        CheckoutRequest request = new CheckoutRequest();
        request.setNotes("Book condition: Good");

        CheckoutResponse response = reservationService.checkout(librarian, reservationId, request);

        assertThat(response.getStatus()).isEqualTo(ReservationStatus.CHECKED_OUT);
        assertThat(response.getDueDate()).isEqualTo(response.getCheckedOutAt().plus(14, ChronoUnit.DAYS));
        assertThat(reservation.getNotes()).isEqualTo("Book condition: Good");
    }

    @Test
    void returnRejectsNonLibrarian() {
        Reservation reservation = Reservation.builder()
                .id(UUID.randomUUID())
                .book(book)
                .user(patron)
                .status(ReservationStatus.CHECKED_OUT)
                .build();
        ReturnRequest request = new ReturnRequest();
        request.setCondition(BookCondition.GOOD);

        assertThatThrownBy(() -> reservationService.returnBook(patron, reservation.getId(), request))
                .isInstanceOf(ForbiddenOperationException.class);
    }

    @Test
    void returnRejectsNonCheckedOutStatus() {
        UUID reservationId = UUID.randomUUID();
        Reservation reservation = Reservation.builder()
                .id(reservationId)
                .book(book)
                .user(patron)
                .status(ReservationStatus.RESERVED)
                .build();
        when(reservationRepository.findById(reservationId)).thenReturn(Optional.of(reservation));
        ReturnRequest request = new ReturnRequest();
        request.setCondition(BookCondition.GOOD);

        assertThatThrownBy(() -> reservationService.returnBook(librarian, reservationId, request))
                .isInstanceOf(InvalidReservationStatusException.class);
    }

    @Test
    void returnOnTimeHasNoLateFee() {
        UUID reservationId = UUID.randomUUID();
        Reservation reservation = Reservation.builder()
                .id(reservationId)
                .book(book)
                .user(patron)
                .status(ReservationStatus.CHECKED_OUT)
                .dueDate(Instant.now(clock).plus(2, ChronoUnit.DAYS))
                .build();
        when(reservationRepository.findById(reservationId)).thenReturn(Optional.of(reservation));
        when(bookRepository.findByIdForUpdate(book.getId())).thenReturn(Optional.of(book));

        ReturnRequest request = new ReturnRequest();
        request.setCondition(BookCondition.GOOD);

        ReturnResponse response = reservationService.returnBook(librarian, reservationId, request);

        assertThat(response.getLateDays()).isZero();
        assertThat(response.getLateFee()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(response.getMessage()).isEqualTo("Book returned successfully");
        assertThat(book.getAvailableCopies()).isEqualTo(3);
    }

    @Test
    void returnLateCalculatesFeeByCalendarDay() {
        UUID reservationId = UUID.randomUUID();
        Instant dueDate = Instant.parse("2025-10-13T15:00:00Z");
        Instant returnedAt = Instant.parse("2025-10-15T10:00:00Z");

        Reservation reservation = Reservation.builder()
                .id(reservationId)
                .book(book)
                .user(patron)
                .status(ReservationStatus.CHECKED_OUT)
                .dueDate(dueDate)
                .build();
        when(reservationRepository.findById(reservationId)).thenReturn(Optional.of(reservation));
        when(bookRepository.findByIdForUpdate(book.getId())).thenReturn(Optional.of(book));

        ReservationService serviceAtReturnTime = new ReservationService(
                reservationRepository, bookRepository, Clock.fixed(returnedAt, ZoneOffset.UTC));

        ReturnRequest request = new ReturnRequest();
        request.setCondition(BookCondition.FAIR);

        ReturnResponse response = serviceAtReturnTime.returnBook(librarian, reservationId, request);

        assertThat(response.getLateDays()).isEqualTo(2);
        assertThat(response.getLateFee()).isEqualByComparingTo(new BigDecimal("2.00"));
        assertThat(response.getMessage()).contains("Late fee of $2.00");
    }

    @Test
    void activeReservationsPopulateDaysUntilExpiryOrDue() {
        Instant now = Instant.now(clock);
        Reservation reserved = Reservation.builder()
                .id(UUID.randomUUID())
                .book(book)
                .user(patron)
                .status(ReservationStatus.RESERVED)
                .reservedAt(now)
                .expiresAt(now.plus(7, ChronoUnit.DAYS))
                .build();
        Reservation checkedOut = Reservation.builder()
                .id(UUID.randomUUID())
                .book(book)
                .user(patron)
                .status(ReservationStatus.CHECKED_OUT)
                .checkedOutAt(now)
                .dueDate(now.plus(5, ChronoUnit.DAYS))
                .build();

        when(reservationRepository.findByUserIdAndStatusInOrderByReservedAtDesc(eq(patron.getId()), any()))
                .thenReturn(List.of(reserved, checkedOut));

        ActiveReservationsResponse response = reservationService.getActiveReservations(patron);

        assertThat(response.getTotalActive()).isEqualTo(2);
        ActiveReservationItem reservedItem = response.getReservations().get(0);
        assertThat(reservedItem.getDaysUntilExpiry()).isEqualTo(7L);
        assertThat(reservedItem.getDaysUntilDue()).isNull();

        ActiveReservationItem checkedOutItem = response.getReservations().get(1);
        assertThat(checkedOutItem.getDaysUntilDue()).isEqualTo(5L);
        assertThat(checkedOutItem.getDaysUntilExpiry()).isNull();
    }

    @Test
    void historyFlagsLateReturnsCorrectly() {
        Instant dueDate = Instant.parse("2025-08-04T14:00:00Z");
        Instant returnedAt = Instant.parse("2025-08-10T09:00:00Z");

        Reservation lateReturn = Reservation.builder()
                .id(UUID.randomUUID())
                .book(book)
                .user(patron)
                .status(ReservationStatus.RETURNED)
                .reservedAt(dueDate.minus(14, ChronoUnit.DAYS))
                .dueDate(dueDate)
                .returnedAt(returnedAt)
                .build();

        Page<Reservation> page = new PageImpl<>(List.of(lateReturn), PageRequest.of(0, 20), 1);
        when(reservationRepository.findHistoryByUserId(eq(patron.getId()), any())).thenReturn(page);

        PagedResponse<HistoryItem> history = reservationService.getHistory(patron, 0, 20);

        assertThat(history.getContent().get(0).isWasLate()).isTrue();
    }
}
