package de.jsc3r.wedrsvp.api.invitation.dto.request;

import de.jsc3r.wedrsvp.api.invitation.dto.DietaryPreferences;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import org.eclipse.microprofile.openapi.annotations.media.Schema;

@Schema(name = "GuestAcceptanceRequest", description = "Acceptance and dietary preferences for one guest")
public record GuestAcceptanceRequest(

        @NotNull
        @Schema(description = "Whether this guest accepts the invitation", required = true, example = "true")
        boolean accepted,

        @NotNull
        @Valid
        @Schema(description = "Dietary preferences for this guest", required = true)
        DietaryPreferences dietaryPreferences
) { }
