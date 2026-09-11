package assembly.general.api.dto.catalog;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.time.Instant;
import java.util.UUID;

@Getter
@Builder
@AllArgsConstructor
public class BookDetailResponse {
    private UUID bookId;
    private String isbn;
    private String title;
    private String author;
    private String genre;
    private Integer publicationYear;
    private String description;
    private String publisher;
    private Integer pageCount;
    private String language;
    private Integer totalCopies;
    private Integer availableCopies;
    private String status;
    private Instant createdAt;
    private Instant updatedAt;
}
