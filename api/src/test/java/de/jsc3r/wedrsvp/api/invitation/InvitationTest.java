package de.jsc3r.wedrsvp.api.invitation;

import de.jsc3r.wedrsvp.api.auth.AllowedResponder;
import jakarta.annotation.security.RolesAllowed;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

class InvitationTest {

    @Test
    void declaresOwnershipPolicyOnlyOnMethods() {
        assertNull(Invitation.class.getAnnotation(AllowedResponder.class));
        for (var method : Invitation.class.getDeclaredMethods()) {
            assertNotNull(method.getAnnotation(AllowedResponder.class));
        }
    }

    @Test
    void allowsGuestsAndAdministrators() {
        assertArrayEquals(new String[]{"guest", "admin"}, Invitation.class.getAnnotation(RolesAllowed.class).value());
    }
}
