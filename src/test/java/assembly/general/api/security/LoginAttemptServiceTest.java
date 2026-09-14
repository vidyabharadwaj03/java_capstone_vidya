package assembly.general.api.security;

import assembly.general.api.exception.TooManyAttemptsException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class LoginAttemptServiceTest {

    private MutableClock clock;
    private LoginAttemptService service;

    @BeforeEach
    void setUp() {
        clock = new MutableClock(Instant.parse("2025-01-01T00:00:00Z"));
        service = new LoginAttemptService(clock);
    }

    @Test
    void allowsLoginBelowThreshold() {
        for (int i = 0; i < 4; i++) {
            service.recordFailure("locked.out@example.com");
        }

        assertThatCode(() -> service.checkAllowed("locked.out@example.com")).doesNotThrowAnyException();
    }

    @Test
    void blocksAfterFiveFailures() {
        for (int i = 0; i < 5; i++) {
            service.recordFailure("locked.out@example.com");
        }

        assertThatThrownBy(() -> service.checkAllowed("locked.out@example.com"))
                .isInstanceOf(TooManyAttemptsException.class);
    }

    @Test
    void successClearsFailureCount() {
        for (int i = 0; i < 5; i++) {
            service.recordFailure("recovered@example.com");
        }
        service.recordSuccess("recovered@example.com");

        assertThatCode(() -> service.checkAllowed("recovered@example.com")).doesNotThrowAnyException();
    }

    @Test
    void windowExpiryResetsFailureCount() {
        for (int i = 0; i < 5; i++) {
            service.recordFailure("expiring@example.com");
        }
        assertThatThrownBy(() -> service.checkAllowed("expiring@example.com"))
                .isInstanceOf(TooManyAttemptsException.class);

        clock.advance(Duration.ofMinutes(16));

        assertThatCode(() -> service.checkAllowed("expiring@example.com")).doesNotThrowAnyException();
    }

    private static final class MutableClock extends Clock {
        private Instant instant;

        MutableClock(Instant instant) {
            this.instant = instant;
        }

        void advance(Duration duration) {
            instant = instant.plus(duration);
        }

        @Override
        public ZoneId getZone() {
            return ZoneOffset.UTC;
        }

        @Override
        public Clock withZone(ZoneId zone) {
            return this;
        }

        @Override
        public Instant instant() {
            return instant;
        }
    }
}
