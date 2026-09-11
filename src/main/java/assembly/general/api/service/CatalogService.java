package assembly.general.api.service;

import assembly.general.api.dto.PagedResponse;
import assembly.general.api.dto.catalog.BookDetailResponse;
import assembly.general.api.dto.catalog.BookSummaryResponse;
import assembly.general.api.entity.Book;
import assembly.general.api.exception.ResourceNotFoundException;
import assembly.general.api.repository.BookRepository;
import assembly.general.api.repository.BookSpecifications;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import java.util.Set;
import java.util.UUID;

@Service
public class CatalogService {

    private static final Set<String> ALLOWED_SORT_FIELDS = Set.of("title", "author", "publicationYear");

    private final BookRepository bookRepository;

    public CatalogService(BookRepository bookRepository) {
        this.bookRepository = bookRepository;
    }

    public PagedResponse<BookSummaryResponse> listBooks(int page, int size, String sortBy, String sortOrder,
                                                         String query, String genre, String isbn, boolean availableOnly) {
        String field = ALLOWED_SORT_FIELDS.contains(sortBy) ? sortBy : "title";
        Sort.Direction direction = "desc".equalsIgnoreCase(sortOrder) ? Sort.Direction.DESC : Sort.Direction.ASC;
        Pageable pageable = PageRequest.of(page, size, Sort.by(direction, field));

        Specification<Book> specification = BookSpecifications.combine(
                BookSpecifications.titleOrAuthorContains(query),
                BookSpecifications.hasGenre(genre),
                BookSpecifications.hasIsbn(isbn),
                BookSpecifications.availableOnly(availableOnly)
        );

        Page<Book> books = bookRepository.findAll(specification, pageable);
        Page<BookSummaryResponse> mapped = books.map(this::toSummary);
        return PagedResponse.from(mapped);
    }

    public BookDetailResponse getBookDetail(UUID bookId) {
        Book book = bookRepository.findById(bookId)
                .orElseThrow(() -> new ResourceNotFoundException("Book not found with ID: " + bookId));
        return toDetail(book);
    }

    private BookSummaryResponse toSummary(Book book) {
        return BookSummaryResponse.builder()
                .bookId(book.getId())
                .isbn(book.getIsbn())
                .title(book.getTitle())
                .author(book.getAuthor())
                .genre(book.getGenre())
                .publicationYear(book.getPublicationYear())
                .description(book.getDescription())
                .totalCopies(book.getTotalCopies())
                .availableCopies(book.getAvailableCopies())
                .status(status(book))
                .build();
    }

    private BookDetailResponse toDetail(Book book) {
        return BookDetailResponse.builder()
                .bookId(book.getId())
                .isbn(book.getIsbn())
                .title(book.getTitle())
                .author(book.getAuthor())
                .genre(book.getGenre())
                .publicationYear(book.getPublicationYear())
                .description(book.getDescription())
                .publisher(book.getPublisher())
                .pageCount(book.getPageCount())
                .language(book.getLanguage())
                .totalCopies(book.getTotalCopies())
                .availableCopies(book.getAvailableCopies())
                .status(status(book))
                .createdAt(book.getCreatedAt())
                .updatedAt(book.getUpdatedAt())
                .build();
    }

    private String status(Book book) {
        return book.getAvailableCopies() > 0 ? "AVAILABLE" : "CHECKED_OUT";
    }
}
