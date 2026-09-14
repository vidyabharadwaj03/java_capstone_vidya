package assembly.general.api.service;

import assembly.general.api.dto.auth.LoginRequest;
import assembly.general.api.dto.auth.LoginResponse;
import assembly.general.api.dto.auth.RegisterRequest;
import assembly.general.api.dto.auth.RegisterResponse;
import assembly.general.api.dto.user.ProfileResponse;
import assembly.general.api.entity.MembershipStatus;
import assembly.general.api.entity.ReservationStatus;
import assembly.general.api.entity.Role;
import assembly.general.api.entity.User;
import assembly.general.api.exception.DuplicateEmailException;
import assembly.general.api.exception.InvalidCredentialsException;
import assembly.general.api.repository.ReservationRepository;
import assembly.general.api.repository.UserRepository;
import assembly.general.api.security.JwtService;
import assembly.general.api.security.LoginAttemptService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.quality.Strictness;
import org.mockito.junit.jupiter.MockitoSettings;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private ReservationRepository reservationRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtService jwtService;

    @Mock
    private LoginAttemptService loginAttemptService;

    @Mock
    private Clock clock;

    @InjectMocks
    private UserService userService;

    private RegisterRequest registerRequest;
    private User user;

    @BeforeEach
    void setUp() {
        when(clock.instant()).thenReturn(Instant.now());

        registerRequest = new RegisterRequest();
        registerRequest.setEmail("new.user@example.com");
        registerRequest.setPassword("SecurePass123!");
        registerRequest.setFirstName("New");
        registerRequest.setLastName("User");
        registerRequest.setPhoneNumber("+1-555-0100");

        user = User.builder()
                .id(UUID.randomUUID())
                .email("new.user@example.com")
                .password("hashed-password")
                .firstName("New")
                .lastName("User")
                .phoneNumber("+1-555-0100")
                .role(Role.PATRON)
                .membershipStatus(MembershipStatus.ACTIVE)
                .memberSince(Instant.now())
                .createdAt(Instant.now())
                .build();
    }

    @Test
    void registerCreatesPatronWithActiveStatus() {
        when(userRepository.existsByEmail("new.user@example.com")).thenReturn(false);
        when(passwordEncoder.encode("SecurePass123!")).thenReturn("hashed-password");
        when(userRepository.save(any(User.class))).thenReturn(user);

        RegisterResponse response = userService.register(registerRequest);

        assertThat(response.getRole()).isEqualTo(Role.PATRON);
        assertThat(response.getMembershipStatus()).isEqualTo(MembershipStatus.ACTIVE);
        assertThat(response.getMessage()).isEqualTo("Registration successful");
    }

    @Test
    void registerRejectsDuplicateEmail() {
        when(userRepository.existsByEmail("new.user@example.com")).thenReturn(true);

        assertThatThrownBy(() -> userService.register(registerRequest))
                .isInstanceOf(DuplicateEmailException.class);

        verify(userRepository, never()).save(any());
    }

    @Test
    void loginSucceedsWithValidCredentials() {
        LoginRequest request = new LoginRequest();
        request.setEmail("new.user@example.com");
        request.setPassword("SecurePass123!");

        when(userRepository.findByEmail("new.user@example.com")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("SecurePass123!", "hashed-password")).thenReturn(true);
        when(jwtService.generateToken(user)).thenReturn("signed-token");
        when(jwtService.getExpirationSeconds()).thenReturn(86400L);

        LoginResponse response = userService.login(request);

        assertThat(response.getAccessToken()).isEqualTo("signed-token");
        assertThat(response.getTokenType()).isEqualTo("Bearer");
        assertThat(response.getExpiresIn()).isEqualTo(86400L);
        assertThat(response.getUser().getEmail()).isEqualTo("new.user@example.com");
    }

    @Test
    void loginFailsForUnknownEmail() {
        LoginRequest request = new LoginRequest();
        request.setEmail("missing@example.com");
        request.setPassword("whatever");

        when(userRepository.findByEmail("missing@example.com")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userService.login(request))
                .isInstanceOf(InvalidCredentialsException.class);
    }

    @Test
    void loginFailsForWrongPassword() {
        LoginRequest request = new LoginRequest();
        request.setEmail("new.user@example.com");
        request.setPassword("WrongPassword1!");

        when(userRepository.findByEmail("new.user@example.com")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("WrongPassword1!", "hashed-password")).thenReturn(false);

        assertThatThrownBy(() -> userService.login(request))
                .isInstanceOf(InvalidCredentialsException.class);
    }

    @Test
    void profileCalculatesActiveAndHistoryCounts() {
        when(reservationRepository.countByUserIdAndStatusIn(user.getId(),
                List.of(ReservationStatus.RESERVED, ReservationStatus.CHECKED_OUT))).thenReturn(2L);
        when(reservationRepository.countByUserIdAndStatus(user.getId(), ReservationStatus.RETURNED))
                .thenReturn(45L);

        ProfileResponse profile = userService.getProfile(user);

        assertThat(profile.getActiveReservations()).isEqualTo(2L);
        assertThat(profile.getBorrowingHistory()).isEqualTo(45L);
        assertThat(profile.getEmail()).isEqualTo(user.getEmail());
    }
}
