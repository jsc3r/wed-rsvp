package de.jsc3r.wedrsvp.api.invitation.dto.response;

import java.util.List;

public record InvitationDetails(
        List<Guest> guests,
        Language language
) { }
