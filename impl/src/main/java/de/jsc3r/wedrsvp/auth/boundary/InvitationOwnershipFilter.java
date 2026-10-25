package de.jsc3r.wedrsvp.auth.boundary;

import de.jsc3r.wedrsvp.api.auth.AllowedResponder;
import de.jsc3r.wedrsvp.auth.control.InvitationAccess;
import jakarta.annotation.Priority;
import jakarta.inject.Inject;
import jakarta.ws.rs.ForbiddenException;
import jakarta.ws.rs.Priorities;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerRequestFilter;
import jakarta.ws.rs.ext.Provider;

@Provider
@AllowedResponder
@Priority(Priorities.AUTHORIZATION)
public class InvitationOwnershipFilter implements ContainerRequestFilter {

    @Inject
    InvitationAccess access;

    @Override
    public void filter(ContainerRequestContext request) {
        var invitationCode = request.getUriInfo().getPathParameters().getFirst("invitation-code");
        if (!access.canAccess(invitationCode)) {
            throw new ForbiddenException();
        }
    }
}
