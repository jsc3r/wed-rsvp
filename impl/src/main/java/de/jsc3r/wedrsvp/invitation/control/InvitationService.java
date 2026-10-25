package de.jsc3r.wedrsvp.invitation.control;

import de.jsc3r.wedrsvp.api.invitation.dto.request.InvitationAcceptanceRequest;
import de.jsc3r.wedrsvp.api.invitation.dto.response.InvitationDetails;
import de.jsc3r.wedrsvp.invitation.control.exception.InvitationNotFoundException;
import de.jsc3r.wedrsvp.invitation.entity.Invitation;
import de.jsc3r.wedrsvp.auth.control.InvitationAccess;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;

@ApplicationScoped
public class InvitationService {

    @Inject
    InvitationMapper mapper;

    @Inject
    InvitationAccess access;

    @Transactional
    public InvitationDetails details(String invitationCode) {
        var invitation = findInvitation(invitationCode);
        return mapper.toInvitationDetails(invitation);
    }

    @Transactional
    public void accept(String invitationCode, InvitationAcceptanceRequest request) {
        var invitation = findInvitation(invitationCode);
        invitation.guests = request.guests().stream()
                .map(mapper::toGuest)
                .toList();
    }

    @Transactional
    public void decline(String invitationCode) {
        var invitation = findInvitation(invitationCode);
        invitation.guests.forEach(guest -> guest.accepted = false);
    }

    private Invitation findInvitation(String invitationCode) {
        var invitation = Invitation.findByInvitationCode(invitationCode)
                .orElseThrow(() -> new InvitationNotFoundException(invitationCode));
        access.requireIdentity(invitation.account == null ? null : invitation.account.id);
        return invitation;
    }
}
