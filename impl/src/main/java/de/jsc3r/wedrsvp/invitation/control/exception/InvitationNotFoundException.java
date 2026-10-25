package de.jsc3r.wedrsvp.invitation.control.exception;

import jakarta.ws.rs.core.Response;

public class InvitationNotFoundException extends InvitationException {

    public InvitationNotFoundException(String invitationCode) {
        super(
                "An invitation with the code %s was not found".formatted(invitationCode),
                invitationCode,
                Response.Status.NOT_FOUND);
    }
}
