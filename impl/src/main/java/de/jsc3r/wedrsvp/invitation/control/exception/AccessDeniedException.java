package de.jsc3r.wedrsvp.invitation.control.exception;

import jakarta.ws.rs.core.Response;

public class AccessDeniedException extends InvitationException {

    public AccessDeniedException(String invitationCode, String ignoredPin) {
        super(
                "The invitation code is either non existent or does not match the pin",
                invitationCode,
                Response.Status.FORBIDDEN);
    }
}
