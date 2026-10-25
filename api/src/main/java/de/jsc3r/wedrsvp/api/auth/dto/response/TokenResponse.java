package de.jsc3r.wedrsvp.api.auth.dto.response;

import org.eclipse.microprofile.openapi.annotations.media.Schema;

import java.util.Set;

@Schema(name = "TokenResponse", description = "A bearer token and the access rights it grants")
public record TokenResponse(
        String accessToken,
        @Schema(examples = "Bearer") String tokenType,
        @Schema(description = "The remaining token lifetime in seconds") long expiresIn,
        Set<String> roles
) { }
