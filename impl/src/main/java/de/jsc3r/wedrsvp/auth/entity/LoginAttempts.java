package de.jsc3r.wedrsvp.auth.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

import java.time.Duration;
import java.time.Instant;

@Embeddable
public class LoginAttempts {

    private static final int MAX_FAILURES = 5;
    private static final Duration LOCK_DURATION = Duration.ofMinutes(15);

    @Column(nullable = false, columnDefinition = "integer default 0")
    public int failures;

    public Instant lockedUntil;

    public boolean isLocked(Instant now) {
        return lockedUntil != null && now.isBefore(lockedUntil);
    }

    public void resetExpiredLock(Instant now) {
        if (lockedUntil != null && !now.isBefore(lockedUntil)) {
            reset();
        }
    }

    public void recordFailure(Instant now) {
        failures++;
        if (failures >= MAX_FAILURES) {
            lockedUntil = now.plus(LOCK_DURATION);
        }
    }

    public void reset() {
        failures = 0;
        lockedUntil = null;
    }
}
