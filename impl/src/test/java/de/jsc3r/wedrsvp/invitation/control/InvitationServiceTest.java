package de.jsc3r.wedrsvp.invitation.control;

import de.jsc3r.wedrsvp.api.invitation.dto.request.GuestAcceptanceRequest;
import de.jsc3r.wedrsvp.api.invitation.dto.request.InvitationAcceptanceRequest;
import de.jsc3r.wedrsvp.api.invitation.dto.response.InvitationDetails;
import de.jsc3r.wedrsvp.invitation.control.exception.AccessDeniedException;
import de.jsc3r.wedrsvp.invitation.control.exception.InvitationNotFoundException;
import de.jsc3r.wedrsvp.invitation.entity.Guest;
import de.jsc3r.wedrsvp.invitation.entity.Invitation;
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
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.mockStatic;

@ExtendWith(MockitoExtension.class)
class InvitationServiceTest {

    private static final String INVITATION_CODE = "abc123";
    private static final String PIN = "123456";

    @Mock
    InvitationMapper mapper;

    @InjectMocks
    InvitationService service;

    @Test
    void returnsDetailsForMatchingPin() {
        Invitation invitation = invitation();
        InvitationDetails details = new InvitationDetails(List.of(),
                de.jsc3r.wedrsvp.api.invitation.dto.response.Language.GERMAN);
        when(mapper.toInvitationDetails(invitation)).thenReturn(details);

        try (MockedStatic<Invitation> invitations = mockInvitation(invitation)) {
            assertSame(details, service.details(INVITATION_CODE, PIN));
            invitations.verify(() -> Invitation.findByInvitationCode(INVITATION_CODE));
        }

        verify(mapper).toInvitationDetails(invitation);
    }

    @Test
    void rejectsDetailsForNonMatchingPin() {
        Invitation invitation = invitation();

        try (MockedStatic<Invitation> ignored = mockInvitation(invitation)) {
            AccessDeniedException exception = assertThrows(
                    AccessDeniedException.class,
                    () -> service.details(INVITATION_CODE, "wrong"));
            assertEquals(INVITATION_CODE, exception.invitationCode());
        }

        verifyNoInteractions(mapper);
    }

    @Test
    void acceptsInvitationAndReplacesGuests() {
        Invitation invitation = invitation();
        GuestAcceptanceRequest firstRequest = mock(GuestAcceptanceRequest.class);
        GuestAcceptanceRequest secondRequest = mock(GuestAcceptanceRequest.class);
        Guest firstGuest = new Guest();
        Guest secondGuest = new Guest();
        when(mapper.toGuest(firstRequest)).thenReturn(firstGuest);
        when(mapper.toGuest(secondRequest)).thenReturn(secondGuest);
        InvitationAcceptanceRequest request = new InvitationAcceptanceRequest(List.of(firstRequest, secondRequest));

        try (MockedStatic<Invitation> ignored = mockInvitation(invitation)) {
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

    private Invitation invitation() {
        Invitation invitation = new Invitation();
        invitation.invitationCode = INVITATION_CODE;
        invitation.pin = PIN;
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
