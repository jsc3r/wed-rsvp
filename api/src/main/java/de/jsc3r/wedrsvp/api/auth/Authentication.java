package de.jsc3r.wedrsvp.api.auth;

import de.jsc3r.wedrsvp.api.auth.dto.request.CredentialsLoginRequest;
import de.jsc3r.wedrsvp.api.auth.dto.request.PinLoginRequest;
import de.jsc3r.wedrsvp.api.auth.dto.response.SessionDetails;
import de.jsc3r.wedrsvp.api.auth.dto.response.TokenResponse;
import jakarta.annotation.security.PermitAll;
import jakarta.annotation.security.RolesAllowed;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.Response;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.enums.SecuritySchemeType;
import org.eclipse.microprofile.openapi.annotations.media.Content;
import org.eclipse.microprofile.openapi.annotations.media.Schema;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;
import org.eclipse.microprofile.openapi.annotations.security.SecurityRequirement;
import org.eclipse.microprofile.openapi.annotations.security.SecurityScheme;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;

@Path("api/auth")
@Consumes("application/json")
@Produces("application/json")
@Tag(name = "Authentication")
@SecurityScheme(securitySchemeName = "bearerAuth", type = SecuritySchemeType.HTTP, scheme = "bearer", bearerFormat = "JWT")
public interface Authentication {

    @POST
    @Path("login/pin")
    @PermitAll
    @Operation(summary = "Log in with an invitation code and four-digit PIN",
            description = "The token contains the authenticated user's stored roles.")
    @APIResponse(responseCode = "200", description = "An access token is returned",
            content = @Content(schema = @Schema(implementation = TokenResponse.class)))
    @APIResponse(responseCode = "400", description = "The login request is invalid")
    @APIResponse(responseCode = "401", description = "The credentials are invalid")
    Response login(@Valid @NotNull PinLoginRequest request);

    @POST
    @Path("login/credentials")
    @PermitAll
    @Operation(summary = "Log in with username and password",
            description = "The token contains the authenticated user's stored roles.")
    @APIResponse(responseCode = "200", description = "An access token is returned",
            content = @Content(schema = @Schema(implementation = TokenResponse.class)))
    @APIResponse(responseCode = "400", description = "The login request is invalid")
    @APIResponse(responseCode = "401", description = "The credentials are invalid")
    Response login(@Valid @NotNull CredentialsLoginRequest request);

    @GET
    @Path("me")
    @RolesAllowed({"guest", "admin"})
    @SecurityRequirement(name = "bearerAuth")
    @Operation(summary = "Get the authenticated identity and access rights")
    @APIResponse(responseCode = "200", description = "The authenticated session is returned",
            content = @Content(schema = @Schema(implementation = SessionDetails.class)))
    @APIResponse(responseCode = "401", description = "A valid authentication token is required")
    SessionDetails me();
}
