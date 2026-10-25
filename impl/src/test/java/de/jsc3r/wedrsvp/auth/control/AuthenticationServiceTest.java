package de.jsc3r.wedrsvp.auth.control;

import de.jsc3r.wedrsvp.auth.entity.UserAccount;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthenticationServiceTest {

    @Mock
    CredentialHasher hasher;

    @InjectMocks
    AuthenticationService service;

    @BeforeEach
    void configureBootstrapAccount() {
        service.username = "admin";
        service.password = "configured-hash";
    }

    @Test
    void skipsBootstrapWhenNeitherSettingIsConfigured() {
        service.username = "";
        service.password = "";

        try (var accounts = mockStatic(UserAccount.class)) {
            service.initialize();

            accounts.verifyNoInteractions();
        }
        verifyNoInteractions(hasher);
    }

    @ParameterizedTest
    @CsvSource(value = {"admin, <unset>", "<unset>, configured-hash"}, nullValues = "<unset>")
    void rejectsIncompleteBootstrapConfiguration(String username, String passwordHash) {
        service.username = username;
        service.password = passwordHash;

        assertThrows(IllegalStateException.class, () -> service.initialize());

        verifyNoInteractions(hasher);
    }

    @ParameterizedTest
    @ValueSource(strings = {"", " ", "\t"})
    void rejectsBlankBootstrapUsername(String username) {
        service.username = username;

        assertThrows(IllegalStateException.class, () -> service.initialize());

        verifyNoInteractions(hasher);
    }

    @Test
    void rejectsOversizedBootstrapUsername() {
        service.username = "a".repeat(129);

        assertThrows(IllegalStateException.class, () -> service.initialize());

        verifyNoInteractions(hasher);
    }

    @Test
    void rejectsInvalidBootstrapPasswordHash() {
        when(hasher.isValidHash("configured-hash")).thenReturn(false);

        assertThrows(IllegalStateException.class, () -> service.initialize());
    }

    @Test
    void keepsExistingAccountPasswordAndRoles() {
        var existing = new UserAccount();
        existing.passwordHash = "stored-hash";
        existing.roles.add("guest");
        when(hasher.isValidHash("configured-hash")).thenReturn(true);

        try (var accounts = mockStatic(UserAccount.class)) {
            accounts.when(() -> UserAccount.findByUsername("admin")).thenReturn(Optional.of(existing));

            service.initialize();

            accounts.verify(() -> UserAccount.findByUsername("admin"));
        }
        assertEquals("stored-hash", existing.passwordHash);
        assertEquals(Set.of("guest"), existing.roles);
    }
}
