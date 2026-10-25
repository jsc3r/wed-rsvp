package de.jsc3r.wedrsvp.auth.entity;

import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LoginAttemptsTest {

    private static final Instant NOW = Instant.parse("2026-10-25T12:00:00Z");

    @Test
    void newAccountIsNotLocked() {
        var attempts = new LoginAttempts();

        attempts.resetExpiredLock(NOW);

        assertFalse(attempts.isLocked(NOW));
        assertEquals(0, attempts.failures);
        assertNull(attempts.lockedUntil);
    }

    @Test
    void fifthFailureLocksForFifteenMinutes() {
        var attempts = new LoginAttempts();

        for (int failure = 1; failure <= 5; failure++) {
            attempts.recordFailure(NOW);
            assertEquals(failure, attempts.failures);
            assertEquals(failure == 5, attempts.isLocked(NOW));
        }
        assertEquals(NOW.plusSeconds(15 * 60), attempts.lockedUntil);
        assertTrue(attempts.isLocked(NOW.plusSeconds(15 * 60 - 1)));
    }

    @Test
    void firstFailureAfterLockExpiryStartsANewAttemptWindow() {
        var attempts = new LoginAttempts();
        attempts.failures = 5;
        attempts.lockedUntil = NOW.minusSeconds(1);

        assertFalse(attempts.isLocked(NOW));
        attempts.resetExpiredLock(NOW);
        attempts.recordFailure(NOW);

        assertEquals(1, attempts.failures);
        assertNull(attempts.lockedUntil);
    }

    @Test
    void lockExpiresAtItsExactDeadline() {
        var attempts = new LoginAttempts();
        attempts.failures = 5;
        attempts.lockedUntil = NOW;

        assertFalse(attempts.isLocked(NOW));
        assertEquals(5, attempts.failures);
        assertEquals(NOW, attempts.lockedUntil);

        attempts.resetExpiredLock(NOW);

        assertEquals(0, attempts.failures);
        assertNull(attempts.lockedUntil);
    }

    @Test
    void resettingExpiredLockPreservesAnActiveLock() {
        var attempts = new LoginAttempts();
        attempts.failures = 5;
        attempts.lockedUntil = NOW.plusSeconds(1);

        attempts.resetExpiredLock(NOW);

        assertTrue(attempts.isLocked(NOW));
        assertEquals(5, attempts.failures);
        assertEquals(NOW.plusSeconds(1), attempts.lockedUntil);
    }

    @Test
    void resetClearsFailuresAndLock() {
        var attempts = new LoginAttempts();
        attempts.failures = 5;
        attempts.lockedUntil = NOW.plusSeconds(900);

        attempts.reset();

        assertEquals(0, attempts.failures);
        assertNull(attempts.lockedUntil);
    }
}
