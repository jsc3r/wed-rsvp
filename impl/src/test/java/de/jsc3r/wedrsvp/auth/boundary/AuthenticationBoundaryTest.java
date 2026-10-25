package de.jsc3r.wedrsvp.auth.boundary;

import de.jsc3r.wedrsvp.api.auth.dto.request.CredentialsLoginRequest;
import de.jsc3r.wedrsvp.api.auth.dto.request.PinLoginRequest;
import jakarta.annotation.security.PermitAll;
import jakarta.annotation.security.RolesAllowed;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class AuthenticationBoundaryTest {

    @Test
    void enforcesPublicLoginsAndProtectedSessionDetails() throws NoSuchMethodException {
        assertNotNull(AuthenticationBoundary.class.getMethod("login", PinLoginRequest.class).getAnnotation(PermitAll.class));
        assertNotNull(AuthenticationBoundary.class.getMethod("login", CredentialsLoginRequest.class).getAnnotation(PermitAll.class));
        assertArrayEquals(new String[]{"guest", "admin"},
                AuthenticationBoundary.class.getMethod("me").getAnnotation(RolesAllowed.class).value());
    }
}
