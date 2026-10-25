package de.jsc3r.wedrsvp.invitation.control.exception;

import jakarta.ws.rs.core.Response;

public abstract class InvitationException extends RuntimeException {

    private final String invitationCode;
    private final Response.Status status;

    protected InvitationException(String message, String invitationCode, Response.Status status) {
        super(message);
        this.invitationCode = invitationCode;
        this.status = status;
    }

    public String invitationCode() {
        return invitationCode;
    }

    public Response.Status status() {
        return status;
    }
}
