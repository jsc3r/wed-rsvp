package de.jsc3r.wedrsvp.auth.control;

import jakarta.enterprise.context.RequestScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.ForbiddenException;
import org.eclipse.microprofile.jwt.JsonWebToken;

@RequestScoped
public class InvitationAccess {

    @Inject
    JsonWebToken token;

    public boolean canAccess(String invitationCode) {
        return token.getGroups().contains("admin")
                || token.getGroups().contains("guest")
                && invitationCode.equals(token.<String>getClaim("invitation_code"));
    }

    public void requireIdentity(Long accountId) {
        if (!token.getGroups().contains("admin")
                && (accountId == null || !("user:" + accountId).equals(token.getSubject()))) {
            throw new ForbiddenException();
        }
    }
}
