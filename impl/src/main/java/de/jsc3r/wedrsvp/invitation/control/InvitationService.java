package de.jsc3r.wedrsvp.invitation.control;

import de.jsc3r.wedrsvp.api.invitation.dto.request.InvitationAcceptanceRequest;
import de.jsc3r.wedrsvp.api.invitation.dto.response.InvitationDetails;
import de.jsc3r.wedrsvp.invitation.control.exception.AccessDeniedException;
import de.jsc3r.wedrsvp.invitation.control.exception.InvitationNotFoundException;
import de.jsc3r.wedrsvp.invitation.entity.Invitation;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;

@ApplicationScoped
public class InvitationService {

    @Inject
    InvitationMapper mapper;

    @Transactional
    public InvitationDetails details(String invitationCode, String pin) {
        Invitation invitation = findInvitation(invitationCode);
        if (!invitation.pin.equals(pin)) {
            throw new AccessDeniedException(invitationCode, pin);
        }
        return mapper.toInvitationDetails(invitation);
    }

    @Transactional
    public void accept(String invitationCode, InvitationAcceptanceRequest request) {
        Invitation invitation = findInvitation(invitationCode);
        invitation.guests = request.guests().stream()
                .map(mapper::toGuest)
                .toList();
    }

    @Transactional
    public void decline(String invitationCode) {
        Invitation invitation = findInvitation(invitationCode);
        invitation.guests.forEach(guest -> guest.accepted = false);
    }

    private Invitation findInvitation(String invitationCode) {
        return Invitation.findByInvitationCode(invitationCode)
                .orElseThrow(() -> new InvitationNotFoundException(invitationCode));
    }
}
