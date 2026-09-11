package assembly.general.api.security;

import assembly.general.api.entity.MembershipStatus;
import assembly.general.api.entity.Role;
import assembly.general.api.entity.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class JwtServiceTest {

    private JwtService jwtService;

    @BeforeEach
    void setUp() {
        jwtService = new JwtService();
        ReflectionTestUtils.setField(jwtService, "secret",
                "test-secret-key-for-jwt-signing-that-is-long-enough-for-hs256");
        ReflectionTestUtils.setField(jwtService, "expirationMs", 86400000L);
    }

    private User buildUser() {
        return User.builder()
                .id(UUID.randomUUID())
                .email("token.user@example.com")
                .password("hashed")
                .firstName("Token")
                .lastName("User")
                .phoneNumber("+1-555-0100")
                .role(Role.PATRON)
                .membershipStatus(MembershipStatus.ACTIVE)
                .memberSince(Instant.now())
                .build();
    }

    @Test
    void generatedTokenContainsSubjectEmail() {
        User user = buildUser();
        String token = jwtService.generateToken(user);

        assertThat(jwtService.extractEmail(token)).isEqualTo(user.getEmail());
    }

    @Test
    void tokenIsValidForMatchingEmail() {
        User user = buildUser();
        String token = jwtService.generateToken(user);

        assertThat(jwtService.isTokenValid(token, user.getEmail())).isTrue();
    }

    @Test
    void tokenIsInvalidForDifferentEmail() {
        User user = buildUser();
        String token = jwtService.generateToken(user);

        assertThat(jwtService.isTokenValid(token, "someone.else@example.com")).isFalse();
    }

    @Test
    void expiredTokenIsRejected() {
        ReflectionTestUtils.setField(jwtService, "expirationMs", -1000L);
        User user = buildUser();
        String token = jwtService.generateToken(user);

        assertThat(jwtService.isTokenValid(token, user.getEmail())).isFalse();
    }

    @Test
    void expirationSecondsMatchesConfiguredMilliseconds() {
        assertThat(jwtService.getExpirationSeconds()).isEqualTo(86400L);
    }
}
