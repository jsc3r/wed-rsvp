package de.jsc3r.wedrsvp.api.auth.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.eclipse.microprofile.openapi.annotations.media.Schema;

@Schema(name = "CredentialsLoginRequest", description = "A user's username and password")
public record CredentialsLoginRequest(
        @NotBlank @Size(max = 128)
        @Schema(description = "The username", required = true)
        String username,
        @NotBlank @Size(max = 72)
        @Schema(description = "The password", required = true, writeOnly = true)
        String password
) { }
