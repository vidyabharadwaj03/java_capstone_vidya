package assembly.general.api.integration;

import assembly.general.api.entity.Book;
import assembly.general.api.entity.MembershipStatus;
import assembly.general.api.entity.Role;
import assembly.general.api.entity.User;
import assembly.general.api.repository.BookRepository;
import assembly.general.api.repository.UserRepository;
import assembly.general.api.service.ReservationService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
class ReservationConcurrencyTest {

    @Autowired
    private ReservationService reservationService;

    @Autowired
    private BookRepository bookRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Test
    void onlyOneConcurrentReservationSucceedsForLastCopy() throws Exception {
        Book book = bookRepository.save(Book.builder()
                .isbn("978-9-CONCURRENCY-" + UUID.randomUUID())
                .title("Concurrency Fixture")
                .author("Test Author")
                .genre("TestGenre")
                .publicationYear(2024)
                .description("fixture")
                .totalCopies(1)
                .availableCopies(1)
                .build());

        int threadCount = 10;
        List<User> patrons = createPatrons(threadCount);

        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        List<Callable<Boolean>> tasks = patrons.stream()
                .map(patron -> (Callable<Boolean>) () -> {
                    try {
                        reservationService.createReservation(patron, book.getId());
                        return true;
                    } catch (RuntimeException ex) {
                        return false;
                    }
                })
                .toList();

        List<Future<Boolean>> results = executor.invokeAll(tasks);
        executor.shutdown();

        AtomicInteger successCount = new AtomicInteger();
        for (Future<Boolean> result : results) {
            if (result.get()) {
                successCount.incrementAndGet();
            }
        }

        assertThat(successCount.get()).isEqualTo(1);

        Book afterConcurrentAttempts = bookRepository.findById(book.getId()).orElseThrow();
        assertThat(afterConcurrentAttempts.getAvailableCopies()).isEqualTo(0);
    }

    private List<User> createPatrons(int count) {
        return java.util.stream.IntStream.range(0, count)
                .mapToObj(i -> userRepository.save(User.builder()
                        .email("concurrency.patron." + UUID.randomUUID() + "@example.com")
                        .password(passwordEncoder.encode("SecurePass123!"))
                        .firstName("Concurrency")
                        .lastName("Patron" + i)
                        .phoneNumber("+1-555-0100")
                        .role(Role.PATRON)
                        .membershipStatus(MembershipStatus.ACTIVE)
                        .memberSince(Instant.now())
                        .build()))
                .toList();
    }
}
