package de.jsc3r.wedrsvp.invitation.control;

import de.jsc3r.wedrsvp.api.invitation.dto.request.GuestAcceptanceRequest;
import de.jsc3r.wedrsvp.api.invitation.dto.request.InvitationAcceptanceRequest;
import de.jsc3r.wedrsvp.api.invitation.dto.response.InvitationDetails;
import de.jsc3r.wedrsvp.auth.control.InvitationAccess;
import de.jsc3r.wedrsvp.auth.entity.UserAccount;
import de.jsc3r.wedrsvp.invitation.control.exception.InvitationNotFoundException;
import de.jsc3r.wedrsvp.invitation.entity.Guest;
import de.jsc3r.wedrsvp.invitation.entity.Invitation;
import jakarta.ws.rs.ForbiddenException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.mockStatic;

@ExtendWith(MockitoExtension.class)
class InvitationServiceTest {

    private static final String INVITATION_CODE = "abc123";

    @Mock
    InvitationMapper mapper;

    @Mock
    InvitationAccess access;

    @InjectMocks
    InvitationService service;

    @Test
    void returnsInvitationDetails() {
        var invitation = invitation();
        var details = new InvitationDetails(List.of(),
                de.jsc3r.wedrsvp.api.invitation.dto.response.Language.GERMAN);
        when(mapper.toInvitationDetails(invitation)).thenReturn(details);

        try (var invitations = mockInvitation(invitation)) {
            assertSame(details, service.details(INVITATION_CODE));
            invitations.verify(() -> Invitation.findByInvitationCode(INVITATION_CODE));
        }

        verify(mapper).toInvitationDetails(invitation);
        verify(access).requireIdentity(invitation.account.id);
    }

    @Test
    void rejectsDetailsForDifferentInvitationIdentity() {
        var invitation = invitation();
        doThrow(new ForbiddenException()).when(access).requireIdentity(invitation.account.id);

        try (var ignored = mockInvitation(invitation)) {
            assertThrows(ForbiddenException.class, () -> service.details(INVITATION_CODE));
        }

        verifyNoInteractions(mapper);
    }

    @Test
    void rejectsDetailsForUnknownInvitation() {
        try (var ignored = mockMissingInvitation()) {
            var exception = assertThrows(
                    InvitationNotFoundException.class,
                    () -> service.details(INVITATION_CODE));
            assertEquals(INVITATION_CODE, exception.invitationCode());
        }

        verifyNoInteractions(mapper);
    }

    @Test
    void acceptsInvitationAndReplacesGuests() {
        var invitation = invitation();
        var firstRequest = mock(GuestAcceptanceRequest.class);
        var secondRequest = mock(GuestAcceptanceRequest.class);
        var firstGuest = new Guest();
        var secondGuest = new Guest();
        when(mapper.toGuest(firstRequest)).thenReturn(firstGuest);
        when(mapper.toGuest(secondRequest)).thenReturn(secondGuest);
        var request = new InvitationAcceptanceRequest(List.of(firstRequest, secondRequest));

        try (var ignored = mockInvitation(invitation)) {
            service.accept(INVITATION_CODE, request);
        }

        assertEquals(List.of(firstGuest, secondGuest), invitation.guests);
        verify(mapper).toGuest(firstRequest);
        verify(mapper).toGuest(secondRequest);
    }

    @Test
    void rejectsAcceptanceForUnknownInvitation() {
        InvitationAcceptanceRequest request = new InvitationAcceptanceRequest(List.of());

        try (MockedStatic<Invitation> ignored = mockMissingInvitation()) {
            InvitationNotFoundException exception = assertThrows(
                    InvitationNotFoundException.class,
                    () -> service.accept(INVITATION_CODE, request));
            assertEquals(INVITATION_CODE, exception.invitationCode());
        }

        verifyNoInteractions(mapper);
    }

    @Test
    void rejectsAcceptanceForAnotherAccount() {
        Invitation invitation = invitation();
        Guest originalGuest = new Guest();
        invitation.guests.add(originalGuest);
        doThrow(new ForbiddenException()).when(access).requireIdentity(invitation.account.id);

        try (MockedStatic<Invitation> ignored = mockInvitation(invitation)) {
            assertThrows(ForbiddenException.class,
                    () -> service.accept(INVITATION_CODE, new InvitationAcceptanceRequest(List.of())));
        }

        assertEquals(List.of(originalGuest), invitation.guests);
        verifyNoInteractions(mapper);
    }

    @Test
    void declinesAllGuests() {
        Invitation invitation = invitation();
        Guest acceptedGuest = new Guest();
        acceptedGuest.accepted = true;
        Guest declinedGuest = new Guest();
        declinedGuest.accepted = false;
        invitation.guests = List.of(acceptedGuest, declinedGuest);

        try (MockedStatic<Invitation> ignored = mockInvitation(invitation)) {
            service.decline(INVITATION_CODE);
        }

        assertFalse(acceptedGuest.accepted);
        assertFalse(declinedGuest.accepted);
    }

    @Test
    void rejectsDeclineForUnknownInvitation() {
        try (MockedStatic<Invitation> ignored = mockMissingInvitation()) {
            assertThrows(InvitationNotFoundException.class, () -> service.decline(INVITATION_CODE));
        }
    }

    @Test
    void rejectsDeclineForAnotherAccount() {
        Invitation invitation = invitation();
        Guest guest = new Guest();
        guest.accepted = true;
        invitation.guests.add(guest);
        doThrow(new ForbiddenException()).when(access).requireIdentity(invitation.account.id);

        try (MockedStatic<Invitation> ignored = mockInvitation(invitation)) {
            assertThrows(ForbiddenException.class, () -> service.decline(INVITATION_CODE));
        }

        assertTrue(guest.accepted);
    }

    private Invitation invitation() {
        Invitation invitation = new Invitation();
        invitation.id = 42L;
        invitation.account = new UserAccount();
        invitation.account.id = 84L;
        invitation.invitationCode = INVITATION_CODE;
        return invitation;
    }

    private MockedStatic<Invitation> mockInvitation(Invitation invitation) {
        MockedStatic<Invitation> invitations = mockStatic(Invitation.class);
        invitations.when(() -> Invitation.findByInvitationCode(INVITATION_CODE)).thenReturn(Optional.of(invitation));
        return invitations;
    }

    private MockedStatic<Invitation> mockMissingInvitation() {
        MockedStatic<Invitation> invitations = mockStatic(Invitation.class);
        invitations.when(() -> Invitation.findByInvitationCode(INVITATION_CODE)).thenReturn(Optional.empty());
        return invitations;
    }
}
