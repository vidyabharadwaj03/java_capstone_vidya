package assembly.general.api.security;

import assembly.general.api.exception.TooManyAttemptsException;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class LoginAttemptService {

    private static final int MAX_ATTEMPTS = 5;
    private static final long WINDOW_MINUTES = 15;

    private final Clock clock;
    private final ConcurrentHashMap<String, Attempt> attemptsByEmail = new ConcurrentHashMap<>();

    public LoginAttemptService(Clock clock) {
        this.clock = clock;
    }

    public void checkAllowed(String email) {
        Attempt attempt = attemptsByEmail.get(normalize(email));
        if (attempt == null) {
            return;
        }
        if (windowExpired(attempt)) {
            attemptsByEmail.remove(normalize(email));
            return;
        }
        if (attempt.count >= MAX_ATTEMPTS) {
            throw new TooManyAttemptsException(
                    "Too many failed login attempts. Try again in " + WINDOW_MINUTES + " minutes.");
        }
    }

    public void recordFailure(String email) {
        attemptsByEmail.compute(normalize(email), (key, existing) -> {
            if (existing == null || windowExpired(existing)) {
                return new Attempt(1, Instant.now(clock));
            }
            return new Attempt(existing.count + 1, existing.windowStart);
        });
    }

    public void recordSuccess(String email) {
        attemptsByEmail.remove(normalize(email));
    }

    private boolean windowExpired(Attempt attempt) {
        return Instant.now(clock).isAfter(attempt.windowStart.plus(WINDOW_MINUTES, ChronoUnit.MINUTES));
    }

    private String normalize(String email) {
        return email == null ? "" : email.toLowerCase();
    }

    private record Attempt(int count, Instant windowStart) {
    }
}
