package assembly.general.api.repository;

import assembly.general.api.entity.MembershipStatus;
import assembly.general.api.entity.Role;
import assembly.general.api.entity.User;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.dao.DataIntegrityViolationException;

import java.time.Instant;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DataJpaTest
class UserRepositoryTest {

    @Autowired
    private UserRepository userRepository;

    private User buildUser(String email) {
        return User.builder()
                .email(email)
                .password("hashed-password")
                .firstName("Jane")
                .lastName("Doe")
                .phoneNumber("+1-555-0100")
                .role(Role.PATRON)
                .membershipStatus(MembershipStatus.ACTIVE)
                .memberSince(Instant.now())
                .build();
    }

    @Test
    void findByEmailReturnsUserWhenPresent() {
        userRepository.saveAndFlush(buildUser("jane.doe@example.com"));

        Optional<User> found = userRepository.findByEmail("jane.doe@example.com");

        assertThat(found).isPresent();
        assertThat(found.get().getFirstName()).isEqualTo("Jane");
    }

    @Test
    void findByEmailReturnsEmptyWhenAbsent() {
        Optional<User> found = userRepository.findByEmail("nobody@example.com");

        assertThat(found).isEmpty();
    }

    @Test
    void existsByEmailReflectsPersistedState() {
        assertThat(userRepository.existsByEmail("new.user@example.com")).isFalse();

        userRepository.saveAndFlush(buildUser("new.user@example.com"));

        assertThat(userRepository.existsByEmail("new.user@example.com")).isTrue();
    }

    @Test
    void duplicateEmailViolatesUniqueConstraint() {
        userRepository.saveAndFlush(buildUser("duplicate@example.com"));

        assertThatThrownBy(() -> userRepository.saveAndFlush(buildUser("duplicate@example.com")))
                .isInstanceOf(DataIntegrityViolationException.class);
    }
}
