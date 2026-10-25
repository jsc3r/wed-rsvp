package de.jsc3r.wedrsvp.invitation.boundary;

import de.jsc3r.wedrsvp.invitation.control.exception.InvitationException;
import jakarta.ws.rs.core.Response;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class InvitationExceptionHandlerTest {

    private InvitationExceptionHandler handler;

    @Mock
    private InvitationException exception;

    @BeforeEach
    void setUp() {
        handler = new InvitationExceptionHandler();
    }

    @Test
    void mapsInvitationNotFoundToNotFound() {
        when(exception.status()).thenReturn(Response.Status.NOT_FOUND);

        try (var response = handler.toResponse(exception)) {
            assertEquals(Response.Status.NOT_FOUND.getStatusCode(), response.getStatus());
        }
    }

    @Test
    void mapsAccessDeniedToForbidden() {
        when(exception.status()).thenReturn(Response.Status.FORBIDDEN);

        try (var response = handler.toResponse(exception)) {
            assertEquals(Response.Status.FORBIDDEN.getStatusCode(), response.getStatus());
        }
    }
}
