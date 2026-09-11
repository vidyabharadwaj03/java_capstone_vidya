package assembly.general.api.integration;

import assembly.general.api.entity.Book;
import assembly.general.api.repository.BookRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class CatalogFlowIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private BookRepository bookRepository;

    @Test
    void browseSearchFilterAndFetchDetail() throws Exception {
        Book book = bookRepository.save(Book.builder()
                .isbn("978-1-ZZ-000000-0")
                .title("Zzyzx Testing Patterns")
                .author("Zach Zephyr")
                .genre("ZZTestGenre")
                .publicationYear(2020)
                .description("A catalog integration test fixture")
                .publisher("Test Press")
                .pageCount(200)
                .language("English")
                .totalCopies(3)
                .availableCopies(1)
                .build());

        mockMvc.perform(get("/api/catalog/books")
                        .param("page", "0")
                        .param("size", "5")
                        .param("sortBy", "title")
                        .param("sortOrder", "asc"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.page").value(0))
                .andExpect(jsonPath("$.size").value(5));

        mockMvc.perform(get("/api/catalog/books").param("genre", "ZZTestGenre"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].title").value("Zzyzx Testing Patterns"));

        mockMvc.perform(get("/api/catalog/books").param("query", "zzyzx"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1));

        mockMvc.perform(get("/api/catalog/books").param("isbn", "978-1-ZZ-000000-0"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].status").value("AVAILABLE"));

        mockMvc.perform(get("/api/catalog/books")
                        .param("genre", "ZZTestGenre")
                        .param("availableOnly", "true"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1));

        mockMvc.perform(get("/api/catalog/books").param("query", "no-such-book-exists"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(0));

        mockMvc.perform(get("/api/catalog/books/" + book.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.isbn").value("978-1-ZZ-000000-0"))
                .andExpect(jsonPath("$.publisher").value("Test Press"));

        mockMvc.perform(get("/api/catalog/books/" + UUID.randomUUID()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("NOT_FOUND"));
    }
}
