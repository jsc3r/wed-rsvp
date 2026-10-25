package de.jsc3r.wedrsvp.api.auth;

import de.jsc3r.wedrsvp.api.auth.dto.request.CredentialsLoginRequest;
import de.jsc3r.wedrsvp.api.auth.dto.request.PinLoginRequest;
import jakarta.annotation.security.PermitAll;
import jakarta.annotation.security.RolesAllowed;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class AuthenticationTest {

    @Test
    void declaresPublicLoginsAndProtectedSessionDetails() throws NoSuchMethodException {
        assertNotNull(Authentication.class.getMethod("login", PinLoginRequest.class).getAnnotation(PermitAll.class));
        assertNotNull(Authentication.class.getMethod("login", CredentialsLoginRequest.class).getAnnotation(PermitAll.class));
        assertArrayEquals(new String[]{"guest", "admin"},
                Authentication.class.getMethod("me").getAnnotation(RolesAllowed.class).value());
    }
}
