package assembly.general.api.service;

import assembly.general.api.dto.auth.LoginRequest;
import assembly.general.api.dto.auth.LoginResponse;
import assembly.general.api.dto.auth.RegisterRequest;
import assembly.general.api.dto.auth.RegisterResponse;
import assembly.general.api.dto.auth.UserSummary;
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
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

@Service
public class UserService {

    private final UserRepository userRepository;
    private final ReservationRepository reservationRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    public UserService(UserRepository userRepository,
                        ReservationRepository reservationRepository,
                        PasswordEncoder passwordEncoder,
                        JwtService jwtService) {
        this.userRepository = userRepository;
        this.reservationRepository = reservationRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
    }

    @Transactional
    public RegisterResponse register(RegisterRequest request) {
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new DuplicateEmailException("Email already exists");
        }

        User user = User.builder()
                .email(request.getEmail())
                .password(passwordEncoder.encode(request.getPassword()))
                .firstName(request.getFirstName())
                .lastName(request.getLastName())
                .phoneNumber(request.getPhoneNumber())
                .role(Role.PATRON)
                .membershipStatus(MembershipStatus.ACTIVE)
                .memberSince(Instant.now())
                .build();

        User saved = userRepository.save(user);

        return RegisterResponse.builder()
                .userId(saved.getId())
                .email(saved.getEmail())
                .firstName(saved.getFirstName())
                .lastName(saved.getLastName())
                .role(saved.getRole())
                .membershipStatus(saved.getMembershipStatus())
                .createdAt(saved.getCreatedAt())
                .message("Registration successful")
                .build();
    }

    public LoginResponse login(LoginRequest request) {
        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new InvalidCredentialsException("Invalid email or password"));

        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw new InvalidCredentialsException("Invalid email or password");
        }

        String token = jwtService.generateToken(user);

        UserSummary summary = UserSummary.builder()
                .userId(user.getId())
                .email(user.getEmail())
                .firstName(user.getFirstName())
                .lastName(user.getLastName())
                .role(user.getRole())
                .build();

        return LoginResponse.builder()
                .accessToken(token)
                .tokenType("Bearer")
                .expiresIn(jwtService.getExpirationSeconds())
                .user(summary)
                .build();
    }

    public ProfileResponse getProfile(User user) {
        long activeReservations = reservationRepository.countByUserIdAndStatusIn(
                user.getId(), List.of(ReservationStatus.RESERVED, ReservationStatus.CHECKED_OUT));
        long borrowingHistory = reservationRepository.countByUserIdAndStatus(
                user.getId(), ReservationStatus.RETURNED);

        return ProfileResponse.builder()
                .userId(user.getId())
                .email(user.getEmail())
                .firstName(user.getFirstName())
                .lastName(user.getLastName())
                .phoneNumber(user.getPhoneNumber())
                .role(user.getRole())
                .membershipStatus(user.getMembershipStatus())
                .memberSince(user.getMemberSince())
                .activeReservations(activeReservations)
                .borrowingHistory(borrowingHistory)
                .build();
    }
}
