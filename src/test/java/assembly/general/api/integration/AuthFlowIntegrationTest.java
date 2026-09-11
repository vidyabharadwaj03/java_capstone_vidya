package assembly.general.api.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.HashMap;
import java.util.Map;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class AuthFlowIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void registerLoginAndFetchProfile() throws Exception {
        Map<String, String> registerBody = new HashMap<>();
        registerBody.put("email", "flow.test@example.com");
        registerBody.put("password", "SecurePass123!");
        registerBody.put("firstName", "Flow");
        registerBody.put("lastName", "Test");
        registerBody.put("phoneNumber", "+1-555-0100");

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(registerBody)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.role").value("PATRON"))
                .andExpect(jsonPath("$.membershipStatus").value("ACTIVE"));

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(registerBody)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("VALIDATION_ERROR"));

        Map<String, String> loginBody = new HashMap<>();
        loginBody.put("email", "flow.test@example.com");
        loginBody.put("password", "SecurePass123!");

        String loginResponse = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginBody)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.tokenType").value("Bearer"))
                .andReturn().getResponse().getContentAsString();

        String token = objectMapper.readTree(loginResponse).get("accessToken").asText();

        mockMvc.perform(get("/api/users/profile")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("flow.test@example.com"))
                .andExpect(jsonPath("$.activeReservations").value(0));

        mockMvc.perform(get("/api/users/profile"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error").value("UNAUTHORIZED"));

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "email", "flow.test@example.com",
                                "password", "WrongPassword123!"
                        ))))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error").value("AUTHENTICATION_FAILED"));
    }
}
