package assembly.general.api.repository;

import assembly.general.api.entity.Book;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DataJpaTest
class BookRepositoryTest {

    @Autowired
    private BookRepository bookRepository;

    @BeforeEach
    void seed() {
        bookRepository.saveAllAndFlush(java.util.List.of(
                book("111", "Clean Code", "Robert Martin", "Technology", 2008, 2),
                book("222", "Refactoring", "Martin Fowler", "Technology", 2018, 0),
                book("333", "1984", "George Orwell", "Fiction", 1949, 3),
                book("444", "Animal Farm", "George Orwell", "Fiction", 1945, 0)
        ));
    }

    private Book book(String isbn, String title, String author, String genre, int year, int availableCopies) {
        return Book.builder()
                .isbn(isbn)
                .title(title)
                .author(author)
                .genre(genre)
                .publicationYear(year)
                .description("description")
                .totalCopies(availableCopies + 1)
                .availableCopies(availableCopies)
                .build();
    }

    @Test
    void findByIsbnReturnsMatch() {
        assertThat(bookRepository.findByIsbn("111")).isPresent();
        assertThat(bookRepository.findByIsbn("does-not-exist")).isEmpty();
    }

    @Test
    void duplicateIsbnViolatesUniqueConstraint() {
        assertThatThrownBy(() -> bookRepository.saveAndFlush(book("111", "Duplicate", "Someone", "Genre", 2000, 1)))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void searchByTitleOrAuthorMatchesEither() {
        Specification<Book> spec = BookSpecifications.titleOrAuthorContains("orwell");
        Page<Book> result = bookRepository.findAll(spec, PageRequest.of(0, 10));

        assertThat(result.getContent()).extracting(Book::getTitle)
                .containsExactlyInAnyOrder("1984", "Animal Farm");
    }

    @Test
    void filterByGenreReturnsExactMatches() {
        Specification<Book> spec = BookSpecifications.hasGenre("Technology");
        Page<Book> result = bookRepository.findAll(spec, PageRequest.of(0, 10));

        assertThat(result.getTotalElements()).isEqualTo(2);
    }

    @Test
    void filterByAvailableOnlyExcludesZeroCopyBooks() {
        Specification<Book> spec = BookSpecifications.availableOnly(true);
        Page<Book> result = bookRepository.findAll(spec, PageRequest.of(0, 10));

        assertThat(result.getContent()).extracting(Book::getIsbn)
                .containsExactlyInAnyOrder("111", "333");
    }

    @Test
    void combinedFiltersNarrowResults() {
        Specification<Book> spec = BookSpecifications.combine(
                BookSpecifications.hasGenre("Fiction"),
                BookSpecifications.availableOnly(true)
        );
        Page<Book> result = bookRepository.findAll(spec, PageRequest.of(0, 10));

        assertThat(result.getContent()).extracting(Book::getIsbn).containsExactly("333");
    }

    @Test
    void sortingByPublicationYearDescending() {
        Page<Book> result = bookRepository.findAll(
                PageRequest.of(0, 10, Sort.by(Sort.Direction.DESC, "publicationYear")));

        assertThat(result.getContent()).extracting(Book::getPublicationYear)
                .containsExactly(2018, 2008, 1949, 1945);
    }

    @Test
    void paginationMetadataReflectsDataset() {
        Page<Book> result = bookRepository.findAll(PageRequest.of(0, 2, Sort.by("title")));

        assertThat(result.getTotalElements()).isEqualTo(4);
        assertThat(result.getTotalPages()).isEqualTo(2);
        assertThat(result.getContent()).hasSize(2);
        assertThat(result.isLast()).isFalse();
    }

    @Test
    void emptySearchReturnsEmptyPageNotError() {
        Specification<Book> spec = BookSpecifications.titleOrAuthorContains("no-match-anywhere");
        Page<Book> result = bookRepository.findAll(spec, PageRequest.of(0, 10));

        assertThat(result.getContent()).isEmpty();
        assertThat(result.getTotalElements()).isZero();
    }
}
