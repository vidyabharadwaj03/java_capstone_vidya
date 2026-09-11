package assembly.general.api.integration;

import assembly.general.api.entity.Book;
import assembly.general.api.entity.Reservation;
import assembly.general.api.repository.BookRepository;
import assembly.general.api.repository.ReservationRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class ReservationFlowIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private BookRepository bookRepository;

    @Autowired
    private ReservationRepository reservationRepository;

    private String registerAndLoginPatron() throws Exception {
        String email = "reservation.patron+" + UUID.randomUUID() + "@example.com";
        Map<String, String> registerBody = new HashMap<>();
        registerBody.put("email", email);
        registerBody.put("password", "SecurePass123!");
        registerBody.put("firstName", "Res");
        registerBody.put("lastName", "Patron");
        registerBody.put("phoneNumber", "+1-555-0111");

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(registerBody)))
                .andExpect(status().isCreated());

        return login(email, "SecurePass123!");
    }

    private String loginLibrarian() throws Exception {
        return login("librarian@library.com", "Librarian123!");
    }

    private String login(String email, String password) throws Exception {
        String response = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("email", email, "password", password))))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(response).get("accessToken").asText();
    }

    private UUID createBook(String suffix, int totalCopies, int availableCopies) {
        Book book = bookRepository.save(Book.builder()
                .isbn("978-9-RES-" + suffix)
                .title("Reservation Fixture " + suffix)
                .author("Test Author")
                .genre("TestGenre")
                .publicationYear(2021)
                .description("fixture")
                .publisher("Test Press")
                .pageCount(100)
                .language("English")
                .totalCopies(totalCopies)
                .availableCopies(availableCopies)
                .build());
        return book.getId();
    }

    @Test
    void fullReservationLifecycle() throws Exception {
        String patronToken = registerAndLoginPatron();
        String librarianToken = loginLibrarian();

        UUID unavailableBookId = createBook("unavailable", 1, 0);
        mockMvc.perform(post("/api/reservations")
                        .header("Authorization", "Bearer " + patronToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("bookId", unavailableBookId))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("BOOK_UNAVAILABLE"));

        UUID firstBookId = createBook("first", 2, 2);
        String createResponse = mockMvc.perform(post("/api/reservations")
                        .header("Authorization", "Bearer " + patronToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("bookId", firstBookId))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("RESERVED"))
                .andReturn().getResponse().getContentAsString();

        JsonNode created = objectMapper.readTree(createResponse);
        UUID reservationId = UUID.fromString(created.get("reservationId").asText());

        Book afterReserve = bookRepository.findById(firstBookId).orElseThrow();
        org.junit.jupiter.api.Assertions.assertEquals(1, afterReserve.getAvailableCopies());

        for (int i = 0; i < 4; i++) {
            UUID bookId = createBook("limit" + i, 1, 1);
            mockMvc.perform(post("/api/reservations")
                            .header("Authorization", "Bearer " + patronToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(Map.of("bookId", bookId))))
                    .andExpect(status().isCreated());
        }

        UUID sixthBookId = createBook("sixth", 1, 1);
        mockMvc.perform(post("/api/reservations")
                        .header("Authorization", "Bearer " + patronToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("bookId", sixthBookId))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("RESERVATION_LIMIT_EXCEEDED"))
                .andExpect(jsonPath("$.currentReservations").value(5));

        mockMvc.perform(post("/api/reservations/" + reservationId + "/checkout")
                        .header("Authorization", "Bearer " + patronToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isForbidden());

        mockMvc.perform(post("/api/reservations/" + reservationId + "/checkout")
                        .header("Authorization", "Bearer " + librarianToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("notes", "Book condition: Good"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CHECKED_OUT"));

        mockMvc.perform(post("/api/reservations/" + reservationId + "/checkout")
                        .header("Authorization", "Bearer " + librarianToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("INVALID_STATUS"));

        Reservation reservation = reservationRepository.findById(reservationId).orElseThrow();
        reservation.setDueDate(Instant.now().minus(3, ChronoUnit.DAYS));
        reservationRepository.save(reservation);

        mockMvc.perform(post("/api/reservations/" + reservationId + "/return")
                        .header("Authorization", "Bearer " + patronToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("condition", "GOOD"))))
                .andExpect(status().isForbidden());

        mockMvc.perform(post("/api/reservations/" + reservationId + "/return")
                        .header("Authorization", "Bearer " + librarianToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("condition", "GOOD", "notes", "fine"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.lateDays").value(3))
                .andExpect(jsonPath("$.lateFee").value(3.00));

        Book afterReturn = bookRepository.findById(firstBookId).orElseThrow();
        org.junit.jupiter.api.Assertions.assertEquals(2, afterReturn.getAvailableCopies());

        mockMvc.perform(get("/api/reservations")
                        .header("Authorization", "Bearer " + patronToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalActive").value(4));

        mockMvc.perform(get("/api/reservations/history")
                        .header("Authorization", "Bearer " + patronToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].status").value("RETURNED"))
                .andExpect(jsonPath("$.content[0].wasLate").value(true));
    }
}
