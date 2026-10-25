package de.jsc3r.wedrsvp.api.auth.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import org.eclipse.microprofile.openapi.annotations.media.Schema;

@Schema(name = "PinLoginRequest", description = "An invitation code and PIN identifying a user")
public record PinLoginRequest(
        @NotBlank @Size(max = 128)
        @Schema(description = "The invitation code", required = true)
        String invitationCode,
        @NotNull @Pattern(regexp = "[0-9]{4}")
        @Schema(description = "The four-digit PIN; leading zeroes are preserved", required = true, examples = "0123", writeOnly = true)
        String pin
) { }
