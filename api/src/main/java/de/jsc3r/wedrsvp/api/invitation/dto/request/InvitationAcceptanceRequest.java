package de.jsc3r.wedrsvp.api.invitation.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import org.eclipse.microprofile.openapi.annotations.media.Schema;

import java.util.List;

@Schema(name = "InvitationAcceptanceRequest", description = "The guest details submitted when an invitation is accepted")
public record InvitationAcceptanceRequest(

        @NotNull
        @Size(min = 1)
        @Valid
        @Schema(description = "The guests attending or declining the invitation", required = true, minItems = 1)
        List<GuestAcceptanceRequest> guests
) { }