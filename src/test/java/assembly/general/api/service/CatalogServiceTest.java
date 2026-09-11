package assembly.general.api.service;

import assembly.general.api.dto.PagedResponse;
import assembly.general.api.dto.catalog.BookDetailResponse;
import assembly.general.api.dto.catalog.BookSummaryResponse;
import assembly.general.api.entity.Book;
import assembly.general.api.exception.ResourceNotFoundException;
import assembly.general.api.repository.BookRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.jpa.domain.Specification;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CatalogServiceTest {

    @Mock
    private BookRepository bookRepository;

    private CatalogService catalogService;

    private Book availableBook;
    private Book unavailableBook;

    @org.junit.jupiter.api.BeforeEach
    void setUp() {
        catalogService = new CatalogService(bookRepository);

        availableBook = Book.builder()
                .id(UUID.randomUUID())
                .isbn("111")
                .title("Clean Code")
                .author("Robert Martin")
                .genre("Technology")
                .publicationYear(2008)
                .description("desc")
                .publisher("Prentice Hall")
                .pageCount(464)
                .language("English")
                .totalCopies(5)
                .availableCopies(2)
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();

        unavailableBook = Book.builder()
                .id(UUID.randomUUID())
                .isbn("222")
                .title("Refactoring")
                .author("Martin Fowler")
                .genre("Technology")
                .publicationYear(2018)
                .totalCopies(3)
                .availableCopies(0)
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();
    }

    @Test
    void listBooksMapsStatusFromAvailableCopies() {
        Page<Book> page = new PageImpl<>(List.of(availableBook, unavailableBook), PageRequest.of(0, 20), 2);
        when(bookRepository.findAll(any(Specification.class), any(PageRequest.class))).thenReturn(page);

        PagedResponse<BookSummaryResponse> result = catalogService.listBooks(
                0, 20, "title", "asc", null, null, null, false);

        assertThat(result.getContent()).hasSize(2);
        assertThat(result.getContent().get(0).getStatus()).isEqualTo("AVAILABLE");
        assertThat(result.getContent().get(1).getStatus()).isEqualTo("CHECKED_OUT");
    }

    @Test
    void listBooksFallsBackToTitleForInvalidSortField() {
        Page<Book> page = new PageImpl<>(List.of(availableBook), PageRequest.of(0, 20), 1);
        when(bookRepository.findAll(any(Specification.class), any(PageRequest.class))).thenReturn(page);

        PagedResponse<BookSummaryResponse> result = catalogService.listBooks(
                0, 20, "notAllowedField", "asc", null, null, null, false);

        assertThat(result.getContent()).hasSize(1);
    }

    @Test
    void getBookDetailReturnsFullMetadata() {
        when(bookRepository.findById(availableBook.getId())).thenReturn(Optional.of(availableBook));

        BookDetailResponse detail = catalogService.getBookDetail(availableBook.getId());

        assertThat(detail.getIsbn()).isEqualTo("111");
        assertThat(detail.getPublisher()).isEqualTo("Prentice Hall");
        assertThat(detail.getStatus()).isEqualTo("AVAILABLE");
    }

    @Test
    void getBookDetailThrowsWhenNotFound() {
        UUID missingId = UUID.randomUUID();
        when(bookRepository.findById(missingId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> catalogService.getBookDetail(missingId))
                .isInstanceOf(ResourceNotFoundException.class);
    }
}
