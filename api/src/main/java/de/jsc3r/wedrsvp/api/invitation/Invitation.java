package de.jsc3r.wedrsvp.api.invitation;

import de.jsc3r.wedrsvp.api.auth.AllowedResponder;
import de.jsc3r.wedrsvp.api.invitation.dto.request.InvitationAcceptanceRequest;
import de.jsc3r.wedrsvp.api.invitation.dto.response.InvitationDetails;
import jakarta.annotation.security.RolesAllowed;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.Response;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.media.Content;
import org.eclipse.microprofile.openapi.annotations.media.Schema;
import org.eclipse.microprofile.openapi.annotations.parameters.Parameter;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;
import org.eclipse.microprofile.openapi.annotations.parameters.RequestBody;
import org.eclipse.microprofile.openapi.annotations.security.SecurityRequirement;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;

@Produces("application/json")
@Path("api/invitation/{invitation-code}")
@Tag(name = "Invitations")
@SecurityRequirement(name = "bearerAuth")
@RolesAllowed({"guest", "admin"})
public interface Invitation {

    @GET
    @Path("")
    @AllowedResponder
    @Operation(summary = "Get details to the invitation")
    @APIResponse(responseCode = "200", description = "The invitation detail is returned")
    @APIResponse(responseCode = "401", description = "A valid authentication token is required")
    @APIResponse(responseCode = "403", description = "The token does not grant access to this invitation")
    @APIResponse(responseCode = "404", description = "The invitation was not found")
    InvitationDetails details(@Parameter(description = "The invitation code", required = true, example = "a1b2c3")
                              @PathParam("invitation-code") String invitationCode);

    @PUT
    @Path("accept")
    @AllowedResponder
    @Consumes("application/json")
    @Operation(summary = "Accept an invitation")
    @APIResponse(responseCode = "202", description = "The invitation was accepted")
    @APIResponse(responseCode = "400", description = "The acceptance request is invalid")
    @APIResponse(responseCode = "401", description = "A valid authentication token is required")
    @APIResponse(responseCode = "403", description = "The token does not grant access to this invitation")
    @APIResponse(responseCode = "404", description = "The invitation was not found")
    Response accept(
            @Parameter(description = "The invitation code", required = true, example = "a1b2c3")
            @PathParam("invitation-code") String invitationCode,
            @RequestBody(
                    description = "Guest acceptance and dietary preferences",
                    content = @Content(schema = @Schema(implementation = InvitationAcceptanceRequest.class)))
            @Valid @NotNull InvitationAcceptanceRequest request);

    @PUT
    @Path("decline")
    @AllowedResponder
    @Operation(summary = "Decline an invitation")
    @APIResponse(responseCode = "200", description = "The invitation was declined")
    @APIResponse(responseCode = "401", description = "A valid authentication token is required")
    @APIResponse(responseCode = "403", description = "The token does not grant access to this invitation")
    @APIResponse(responseCode = "404", description = "The invitation was not found")
    Response decline(
            @Parameter(description = "The invitation code", required = true, example = "a1b2c3")
            @PathParam("invitation-code") String invitationCode);
}
