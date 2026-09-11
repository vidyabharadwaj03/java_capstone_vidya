package assembly.general.api.controllers;

import assembly.general.api.dto.PagedResponse;
import assembly.general.api.dto.catalog.BookDetailResponse;
import assembly.general.api.dto.catalog.BookSummaryResponse;
import assembly.general.api.service.CatalogService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/catalog")
public class CatalogController {

    private final CatalogService catalogService;

    public CatalogController(CatalogService catalogService) {
        this.catalogService = catalogService;
    }

    @GetMapping("/books")
    public ResponseEntity<PagedResponse<BookSummaryResponse>> listBooks(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "title") String sortBy,
            @RequestParam(defaultValue = "asc") String sortOrder,
            @RequestParam(required = false) String query,
            @RequestParam(required = false) String genre,
            @RequestParam(required = false) String isbn,
            @RequestParam(defaultValue = "false") boolean availableOnly) {
        return ResponseEntity.ok(catalogService.listBooks(page, size, sortBy, sortOrder, query, genre, isbn, availableOnly));
    }

    @GetMapping("/books/{bookId}")
    public ResponseEntity<BookDetailResponse> getBook(@PathVariable("bookId") UUID bookId) {
        return ResponseEntity.ok(catalogService.getBookDetail(bookId));
    }
}
