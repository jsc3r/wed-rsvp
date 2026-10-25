package de.jsc3r.wedrsvp.invitation.boundary;

import de.jsc3r.wedrsvp.invitation.control.exception.InvitationException;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;
import org.jboss.logging.Logger;

@Provider
@Produces(MediaType.APPLICATION_JSON)
public class InvitationExceptionHandler implements ExceptionMapper<InvitationException> {

    private static final Logger LOG = Logger.getLogger(InvitationExceptionHandler.class);

    @Override
    public Response toResponse(InvitationException exception) {
        LOG.debugf("Invitation request failed with status %s", exception.status());
        return Response.status(exception.status()).build();
    }
}
