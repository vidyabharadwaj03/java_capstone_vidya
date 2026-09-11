package assembly.general.api.repository;

import assembly.general.api.entity.Book;
import assembly.general.api.entity.MembershipStatus;
import assembly.general.api.entity.Reservation;
import assembly.general.api.entity.ReservationStatus;
import assembly.general.api.entity.Role;
import assembly.general.api.entity.User;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
class ReservationRepositoryTest {

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private ReservationRepository reservationRepository;

    private User persistUser(String email) {
        User user = User.builder()
                .email(email)
                .password("hashed")
                .firstName("First")
                .lastName("Last")
                .phoneNumber("+1-555-0100")
                .role(Role.PATRON)
                .membershipStatus(MembershipStatus.ACTIVE)
                .memberSince(Instant.now())
                .build();
        return entityManager.persistAndFlush(user);
    }

    private Book persistBook(String isbn) {
        Book book = Book.builder()
                .isbn(isbn)
                .title("Title " + isbn)
                .author("Author")
                .genre("Genre")
                .publicationYear(2020)
                .description("description")
                .totalCopies(3)
                .availableCopies(2)
                .build();
        return entityManager.persistAndFlush(book);
    }

    private Reservation persistReservation(User user, Book book, ReservationStatus status,
                                            Instant reservedAt, Instant returnedAt) {
        Reservation reservation = Reservation.builder()
                .user(user)
                .book(book)
                .status(status)
                .reservedAt(reservedAt)
                .expiresAt(reservedAt.plus(7, ChronoUnit.DAYS))
                .returnedAt(returnedAt)
                .build();
        return entityManager.persistAndFlush(reservation);
    }

    @Test
    void findsActiveReservationsForUserOrderedByMostRecent() {
        User user = persistUser("active.user@example.com");
        Book book = persistBook("ACTIVE-1");
        Instant now = Instant.now();

        persistReservation(user, book, ReservationStatus.RESERVED, now.minus(2, ChronoUnit.DAYS), null);
        persistReservation(user, book, ReservationStatus.CHECKED_OUT, now.minus(1, ChronoUnit.DAYS), null);
        persistReservation(user, book, ReservationStatus.RETURNED, now.minus(5, ChronoUnit.DAYS), now);

        List<Reservation> active = reservationRepository.findByUserIdAndStatusInOrderByReservedAtDesc(
                user.getId(), List.of(ReservationStatus.RESERVED, ReservationStatus.CHECKED_OUT));

        assertThat(active).hasSize(2);
        assertThat(active.get(0).getStatus()).isEqualTo(ReservationStatus.CHECKED_OUT);
    }

    @Test
    void countsActiveReservationsAccurately() {
        User user = persistUser("counter@example.com");
        Book book = persistBook("COUNT-1");
        Instant now = Instant.now();

        for (int i = 0; i < 3; i++) {
            persistReservation(user, book, ReservationStatus.RESERVED, now, null);
        }
        persistReservation(user, book, ReservationStatus.RETURNED, now, now);

        long count = reservationRepository.countByUserIdAndStatusIn(
                user.getId(), List.of(ReservationStatus.RESERVED, ReservationStatus.CHECKED_OUT));

        assertThat(count).isEqualTo(3);
    }

    @Test
    void findsHistoryOrderedByReturnedOrReservedDateDescending() {
        User user = persistUser("history@example.com");
        Book book = persistBook("HIST-1");
        Instant now = Instant.now();

        persistReservation(user, book, ReservationStatus.RETURNED, now.minus(10, ChronoUnit.DAYS), now.minus(9, ChronoUnit.DAYS));
        persistReservation(user, book, ReservationStatus.RETURNED, now.minus(3, ChronoUnit.DAYS), now.minus(1, ChronoUnit.DAYS));
        persistReservation(user, book, ReservationStatus.RESERVED, now, null);

        Page<Reservation> history = reservationRepository.findHistoryByUserId(user.getId(), PageRequest.of(0, 10));

        assertThat(history.getTotalElements()).isEqualTo(3);
        assertThat(history.getContent().get(0).getReservedAt()).isEqualTo(now);
        assertThat(history.getContent().get(2).getReservedAt()).isEqualTo(now.minus(10, ChronoUnit.DAYS));
    }

    @Test
    void resolvesBookAndUserAssociations() {
        User user = persistUser("relations@example.com");
        Book book = persistBook("REL-1");
        Reservation reservation = persistReservation(user, book, ReservationStatus.RESERVED, Instant.now(), null);

        entityManager.clear();

        Reservation reloaded = reservationRepository.findById(reservation.getId()).orElseThrow();

        assertThat(reloaded.getBook().getIsbn()).isEqualTo("REL-1");
        assertThat(reloaded.getUser().getEmail()).isEqualTo("relations@example.com");
    }

    @Test
    void findsExpiredReservationsPastExpiryDate() {
        User user = persistUser("expiry@example.com");
        Book book = persistBook("EXP-1");
        Instant now = Instant.now();

        Reservation expired = persistReservation(user, book, ReservationStatus.RESERVED,
                now.minus(10, ChronoUnit.DAYS), null);
        persistReservation(user, book, ReservationStatus.RESERVED, now, null);

        List<Reservation> results = reservationRepository.findByStatusAndExpiresAtBefore(
                ReservationStatus.RESERVED, now);

        assertThat(results).extracting(Reservation::getId).containsExactly(expired.getId());
    }

    @Test
    void emptyResultsReturnEmptyListNotNull() {
        List<Reservation> results = reservationRepository.findByUserIdAndStatusInOrderByReservedAtDesc(
                UUID.randomUUID(), List.of(ReservationStatus.RESERVED));

        assertThat(results).isNotNull().isEmpty();
    }
}
