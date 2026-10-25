package de.jsc3r.wedrsvp.api.invitation.dto;

import org.eclipse.microprofile.openapi.annotations.media.Schema;

@Schema(name = "DietaryPreferences", description = "Dietary restrictions for a guest")
public record DietaryPreferences(

        @Schema(description = "The guest does not eat gluten", required = true)
        boolean noGluten,

        @Schema(description = "The guest does not eat lactose", required = true)
        boolean noLactose,

        @Schema(description = "The guest does not eat eggs", required = true)
        boolean noEggs,

        @Schema(description = "The guest does not eat fish", required = true)
        boolean noFish,

        @Schema(description = "The guest does not eat chicken", required = true)
        boolean noChicken,

        @Schema(description = "The guest does not eat meat", required = true)
        boolean noMeat,

        @Schema(description = "The guest does not eat nuts", required = true)
        boolean noNuts
) { }
