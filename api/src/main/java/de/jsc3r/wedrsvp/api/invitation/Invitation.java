package de.jsc3r.wedrsvp.api.invitation;

import de.jsc3r.wedrsvp.api.invitation.dto.request.InvitationAcceptanceRequest;
import de.jsc3r.wedrsvp.api.invitation.dto.response.InvitationDetails;
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
import org.eclipse.microprofile.openapi.annotations.tags.Tag;

@Produces("application/json")
@Path("api/invitation/{invitation-code}")
@Tag(name = "Invitations")
public interface Invitation {

    @POST
    @Path("")
    @Operation(summary = "Get details to the invitation")
    @APIResponse(responseCode = "200", description = "The invitation detail is returned")
    @APIResponse(responseCode = "401", description = "The invitation code or the pin were not correct")
    InvitationDetails details(@Parameter(description = "The invitation code", required = true, example = "a1b2c3")
                              @PathParam("invitation-code") String invitationCode,
                              @Parameter(description = "A pin matching the invitation code", required = true, example = "123456")
                              @QueryParam("pin") String pin);

    @PUT
    @Path("accept")
    @Consumes("application/json")
    @Operation(summary = "Accept an invitation")
    @APIResponse(responseCode = "202", description = "The invitation was accepted")
    @APIResponse(responseCode = "400", description = "The acceptance request is invalid")
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
    @Operation(summary = "Decline an invitation")
    @APIResponse(responseCode = "200", description = "The invitation was declined")
    @APIResponse(responseCode = "404", description = "The invitation was not found")
    Response decline(
            @Parameter(description = "The invitation code", required = true, example = "a1b2c3")
            @PathParam("invitation-code") String invitationCode);
}
