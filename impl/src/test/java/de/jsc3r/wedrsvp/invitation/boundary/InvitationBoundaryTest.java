package de.jsc3r.wedrsvp.invitation.boundary;

import de.jsc3r.wedrsvp.api.invitation.dto.request.InvitationAcceptanceRequest;
import de.jsc3r.wedrsvp.api.invitation.dto.response.InvitationDetails;
import de.jsc3r.wedrsvp.invitation.control.InvitationService;
import jakarta.ws.rs.core.Response;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
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
    void returnsDetailsFromService() {
        InvitationDetails details = new InvitationDetails(java.util.List.of(),
                de.jsc3r.wedrsvp.api.invitation.dto.response.Language.GERMAN);
        when(service.details(INVITATION_CODE, "123456")).thenReturn(details);

        assertSame(details, boundary.details(INVITATION_CODE, "123456"));

        verify(service).details(INVITATION_CODE, "123456");
    }

    @Test
    void acceptsInvitationAndReturnsAccepted() {
        InvitationAcceptanceRequest request = new InvitationAcceptanceRequest(java.util.List.of());

        try (Response response = boundary.accept(INVITATION_CODE, request)) {
            assertEquals(Response.Status.ACCEPTED.getStatusCode(), response.getStatus());
        }

        verify(service).accept(INVITATION_CODE, request);
    }

    @Test
    void declinesInvitationAndReturnsOk() {
        try (Response response = boundary.decline(INVITATION_CODE)) {
            assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
        }

        verify(service).decline(INVITATION_CODE);
    }
}
