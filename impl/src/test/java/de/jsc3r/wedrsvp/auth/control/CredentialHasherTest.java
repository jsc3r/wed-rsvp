package de.jsc3r.wedrsvp.auth.control;

import io.quarkus.elytron.security.common.BcryptUtil;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CredentialHasherTest {

    private static final String PIN = "0123";
    private static final String HASH = BcryptUtil.bcryptHash(PIN, 12);
    private static final CredentialHasher HASHER = new CredentialHasher();

    @ParameterizedTest
    @ValueSource(strings = {"$2a$", "$2b$", "$2y$"})
    void acceptsCompatibleBcryptVariants(String prefix) {
        var hash = prefix + HASH.substring(4);

        assertTrue(HASHER.isValidHash(hash));
        assertTrue(HASHER.matches(PIN, hash));
    }

    @ParameterizedTest
    @NullSource
    @ValueSource(strings = {"", "0123", "$2a$12$malformed", "$argon2id$invalid"})
    void rejectsMissingPlaintextAndMalformedHashes(String hash) {
        assertFalse(HASHER.isValidHash(hash));
        assertFalse(HASHER.matches(PIN, hash));
    }

    @ParameterizedTest
    @ValueSource(strings = {"$2x$12$", "$2a$10$", "$2a$11$", "$2a$17$"})
    void rejectsUnsupportedBcryptVersionsAndCosts(String prefix) {
        var hash = prefix + HASH.substring(7);

        assertFalse(HASHER.isValidHash(hash));
        assertFalse(HASHER.matches(PIN, hash));
    }

    @Test
    void rejectsWrongCredential() {
        assertFalse(HASHER.matches("9999", HASH));
    }

    @Test
    void rejectsCredentialsExceedingBcryptByteLimit() {
        assertFalse(HASHER.matches("a".repeat(73), HASH));
        assertFalse(HASHER.matches("é".repeat(37), HASH));
    }

    @Test
    void acceptsExactlySeventyTwoUtf8BytesAndRejectsATruncatedMatch() {
        var credential = "é".repeat(36);
        var hash = BcryptUtil.bcryptHash(credential, 12);

        assertTrue(HASHER.matches(credential, hash));
        assertFalse(HASHER.matches(credential + "a", hash));
    }
}
