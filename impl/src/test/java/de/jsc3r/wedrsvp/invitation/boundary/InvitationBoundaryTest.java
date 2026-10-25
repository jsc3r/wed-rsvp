package de.jsc3r.wedrsvp.invitation.boundary;

import de.jsc3r.wedrsvp.api.auth.AllowedResponder;
import de.jsc3r.wedrsvp.api.invitation.dto.request.InvitationAcceptanceRequest;
import de.jsc3r.wedrsvp.api.invitation.dto.response.InvitationDetails;
import de.jsc3r.wedrsvp.invitation.control.InvitationService;
import jakarta.annotation.security.RolesAllowed;
import jakarta.ws.rs.core.Response;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class InvitationBoundaryTest {

    private static final String INVITATION_CODE = "abc123";

    @Mock
    InvitationService service;

    @InjectMocks
    InvitationBoundary boundary;

    @Test
    void inheritsOwnershipPolicyWithoutRedeclaringIt() {
        assertNull(InvitationBoundary.class.getAnnotation(AllowedResponder.class));
        for (var method : InvitationBoundary.class.getDeclaredMethods()) {
            assertNull(method.getAnnotation(AllowedResponder.class));
        }
    }

    @Test
    void allowsGuestsAndAdministrators() {
        assertArrayEquals(new String[]{"guest", "admin"}, InvitationBoundary.class.getAnnotation(RolesAllowed.class).value());
    }

    @Test
    void returnsDetailsFromService() {
        var details = new InvitationDetails(java.util.List.of(),
                de.jsc3r.wedrsvp.api.invitation.dto.response.Language.GERMAN);
        when(service.details(INVITATION_CODE)).thenReturn(details);

        assertSame(details, boundary.details(INVITATION_CODE));

        verify(service).details(INVITATION_CODE);
    }

    @Test
    void acceptsInvitationAndReturnsAccepted() {
        var request = new InvitationAcceptanceRequest(java.util.List.of());

        try (var response = boundary.accept(INVITATION_CODE, request)) {
            assertEquals(Response.Status.ACCEPTED.getStatusCode(), response.getStatus());
        }

        verify(service).accept(INVITATION_CODE, request);
    }

    @Test
    void declinesInvitationAndReturnsOk() {
        try (var response = boundary.decline(INVITATION_CODE)) {
            assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
        }

        verify(service).decline(INVITATION_CODE);
    }
}
