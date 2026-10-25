package de.jsc3r.wedrsvp.api.auth.dto.response;

import org.eclipse.microprofile.openapi.annotations.media.Schema;

import java.util.Set;

@Schema(name = "SessionDetails", description = "The identity and access rights of the authenticated user")
public record SessionDetails(
        String subject,
        Set<String> roles,
        @Schema(description = "The invitation linked to this account; absent if no invitation is linked") String invitationCode
) { }
