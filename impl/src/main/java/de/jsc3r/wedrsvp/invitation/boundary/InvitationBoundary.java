package de.jsc3r.wedrsvp.invitation.boundary;

import de.jsc3r.wedrsvp.api.invitation.Invitation;
import de.jsc3r.wedrsvp.api.invitation.dto.request.InvitationAcceptanceRequest;
import de.jsc3r.wedrsvp.api.invitation.dto.response.InvitationDetails;
import de.jsc3r.wedrsvp.invitation.control.InvitationService;
import jakarta.inject.Inject;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.core.Response;

public class InvitationBoundary implements Invitation {

    @Inject
    InvitationService service;

    @Override
    public InvitationDetails details(String invitationCode, String pin) {
        return service.details(invitationCode, pin);
    }

    @Override
    @Consumes("application/json")
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
