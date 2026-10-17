package de.jsc3r.wedrsvp.invitation.control.exception;

import jakarta.ws.rs.core.Response;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;

class InvitationExceptionTest {

    @Test
    void describesMissingInvitation() {
        InvitationNotFoundException exception = new InvitationNotFoundException("unknown");

        assertAll(
                () -> assertEquals("unknown", exception.invitationCode()),
                () -> assertEquals("An invitation with the code unknown was not found", exception.getMessage()),
                () -> assertEquals(Response.Status.NOT_FOUND, exception.status()));
    }

    @Test
    void describesDeniedAccess() {
        AccessDeniedException exception = new AccessDeniedException("known", "wrong");

        assertAll(
                () -> assertEquals("known", exception.invitationCode()),
                () -> assertEquals(
                        "The invitation code is either non existent or does not match the pin",
                        exception.getMessage()),
                () -> assertEquals(Response.Status.FORBIDDEN, exception.status()));
    }
}
