package de.jsc3r.wedrsvp.invitation.boundary;

import de.jsc3r.wedrsvp.api.invitation.Invitation;
import de.jsc3r.wedrsvp.api.invitation.dto.request.InvitationAcceptanceRequest;
import de.jsc3r.wedrsvp.api.invitation.dto.response.InvitationDetails;
import de.jsc3r.wedrsvp.invitation.control.InvitationService;
import jakarta.annotation.security.RolesAllowed;
import jakarta.inject.Inject;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.ws.rs.core.Response;

@RolesAllowed({"guest", "admin"})
public class InvitationBoundary implements Invitation {

    @Inject
    InvitationService service;

    @Override
    public InvitationDetails details(String invitationCode) {
        return service.details(invitationCode);
    }

    @Override
    public Response accept(String invitationCode, @Valid @NotNull InvitationAcceptanceRequest request) {
        service.accept(invitationCode, request);
        return Response.accepted().build();
    }

    @Override
    public Response decline(String invitationCode) {
        service.decline(invitationCode);
        return Response.ok().build();
    }
}
