package assembly.general.api.config;

import assembly.general.api.entity.Book;
import assembly.general.api.entity.MembershipStatus;
import assembly.general.api.entity.Role;
import assembly.general.api.entity.User;
import assembly.general.api.repository.BookRepository;
import assembly.general.api.repository.UserRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.List;

@Component
@Profile("dev")
public class DevDataSeeder implements CommandLineRunner {

    private final BookRepository bookRepository;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public DevDataSeeder(BookRepository bookRepository, UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.bookRepository = bookRepository;
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public void run(String... args) {
        if (bookRepository.count() == 0) {
            bookRepository.saveAll(List.of(
                    book("978-0-13-468599-1", "Clean Code", "Robert C. Martin", "Technology", 2008,
                            "A handbook of agile software craftsmanship", "Prentice Hall", 464, "English", 5, 2),
                    book("978-0-13-475759-9", "Refactoring", "Martin Fowler", "Technology", 2018,
                            "Improving the design of existing code", "Addison-Wesley", 448, "English", 3, 0),
                    book("978-0-596-00712-6", "Head First Design Patterns", "Eric Freeman", "Technology", 2004,
                            "A brain-friendly guide to design patterns", "O'Reilly", 694, "English", 4, 4),
                    book("978-0-452-28423-4", "1984", "George Orwell", "Fiction", 1949,
                            "A dystopian social science fiction novel", "Secker & Warburg", 328, "English", 6, 3),
                    book("978-0-14-118280-1", "Pride and Prejudice", "Jane Austen", "Fiction", 1813,
                            "A romantic novel of manners", "T. Egerton", 279, "English", 2, 0),
                    book("978-0-06-231609-7", "Sapiens", "Yuval Noah Harari", "History", 2011,
                            "A brief history of humankind", "Harper", 443, "English", 3, 3),
                    book("978-0-7432-7356-5", "The Da Vinci Code", "Dan Brown", "Fiction", 2003,
                            "A mystery thriller novel", "Doubleday", 454, "English", 4, 1)
            ));
        }

        if (userRepository.findByEmail("librarian@library.com").isEmpty()) {
            userRepository.save(User.builder()
                    .email("librarian@library.com")
                    .password(passwordEncoder.encode("Librarian123!"))
                    .firstName("Lib")
                    .lastName("Rarian")
                    .phoneNumber("+1-555-0000")
                    .role(Role.LIBRARIAN)
                    .membershipStatus(MembershipStatus.ACTIVE)
                    .memberSince(Instant.now())
                    .build());
        }
    }

    private Book book(String isbn, String title, String author, String genre, int year, String description,
                       String publisher, int pageCount, String language, int totalCopies, int availableCopies) {
        return Book.builder()
                .isbn(isbn)
                .title(title)
                .author(author)
                .genre(genre)
                .publicationYear(year)
                .description(description)
                .publisher(publisher)
                .pageCount(pageCount)
                .language(language)
                .totalCopies(totalCopies)
                .availableCopies(availableCopies)
                .build();
    }
}
