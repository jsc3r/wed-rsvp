package de.jsc3r.wedrsvp.auth.boundary;

import de.jsc3r.wedrsvp.api.auth.Authentication;
import de.jsc3r.wedrsvp.api.auth.dto.request.CredentialsLoginRequest;
import de.jsc3r.wedrsvp.api.auth.dto.request.PinLoginRequest;
import de.jsc3r.wedrsvp.api.auth.dto.response.SessionDetails;
import de.jsc3r.wedrsvp.api.auth.dto.response.TokenResponse;
import de.jsc3r.wedrsvp.auth.control.AuthenticationService;
import jakarta.annotation.security.PermitAll;
import jakarta.annotation.security.RolesAllowed;
import jakarta.inject.Inject;
import jakarta.ws.rs.core.Response;
import org.eclipse.microprofile.jwt.JsonWebToken;

public class AuthenticationBoundary implements Authentication {

    @Inject
    AuthenticationService service;

    @Inject
    JsonWebToken token;

    @Override
    @PermitAll
    public Response login(PinLoginRequest request) {
        return tokenResponse(service.login(request));
    }

    @Override
    @PermitAll
    public Response login(CredentialsLoginRequest request) {
        return tokenResponse(service.login(request));
    }

    @Override
    @RolesAllowed({"guest", "admin"})
    public SessionDetails me() {
        return new SessionDetails(token.getSubject(), token.getGroups(), token.getClaim("invitation_code"));
    }

    private Response tokenResponse(TokenResponse response) {
        return Response.ok(response).header("Cache-Control", "no-store").header("Pragma", "no-cache").build();
    }
}
