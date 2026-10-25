package de.jsc3r.wedrsvp.invitation.control;

import de.jsc3r.wedrsvp.api.invitation.dto.request.GuestAcceptanceRequest;
import de.jsc3r.wedrsvp.invitation.entity.DietaryPreferences;
import de.jsc3r.wedrsvp.invitation.entity.Guest;
import de.jsc3r.wedrsvp.invitation.entity.Invitation;
import de.jsc3r.wedrsvp.invitation.entity.Language;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class InvitationMapperTest {

    @Mock
    GuestAcceptanceRequest guestRequest;

    @Mock
    de.jsc3r.wedrsvp.api.invitation.dto.DietaryPreferences dietaryPreferences;

    private final InvitationMapper mapper = new InvitationMapper();

    @Test
    void mapsAcceptanceRequestToGuest() {
        when(guestRequest.accepted()).thenReturn(true);
        when(guestRequest.dietaryPreferences()).thenReturn(dietaryPreferences);
        stubDietaryPreferences();

        var guest = mapper.toGuest(guestRequest);

        assertAll(
                () -> assertTrue(guest.accepted),
                () -> assertDietaryPreferences(guest.dietaryPreferences));
    }

    @Test
    void mapsInvitationToDetails() {
        var invitation = new Invitation();
        invitation.language = Language.SPANISH;

        var guest = new Guest();
        guest.firstName = "Bilbo";
        guest.lastName = "Baggins";
        guest.accepted = true;
        guest.dietaryPreferences = dietaryPreferences();
        invitation.guests = List.of(guest);

        var details = mapper.toInvitationDetails(invitation);
        var guestDetails = details.guests().getFirst();

        assertAll(
                () -> assertEquals(Language.SPANISH.name(), details.language().name()),
                () -> assertEquals("Bilbo", guestDetails.firstName),
                () -> assertEquals("Baggins", guestDetails.lastName),
                () -> assertTrue(guestDetails.accepted),
                () -> assertAll(
                        () -> assertTrue(guestDetails.dietaryPreferences.noGluten()),
                        () -> assertTrue(guestDetails.dietaryPreferences.noLactose()),
                        () -> assertTrue(guestDetails.dietaryPreferences.noEggs()),
                        () -> assertTrue(guestDetails.dietaryPreferences.noFish()),
                        () -> assertTrue(guestDetails.dietaryPreferences.noChicken()),
                        () -> assertTrue(guestDetails.dietaryPreferences.noMeat()),
                        () -> assertTrue(guestDetails.dietaryPreferences.noNuts())));
    }

    private void stubDietaryPreferences() {
        when(dietaryPreferences.noGluten()).thenReturn(true);
        when(dietaryPreferences.noLactose()).thenReturn(true);
        when(dietaryPreferences.noEggs()).thenReturn(true);
        when(dietaryPreferences.noFish()).thenReturn(true);
        when(dietaryPreferences.noChicken()).thenReturn(true);
        when(dietaryPreferences.noMeat()).thenReturn(true);
        when(dietaryPreferences.noNuts()).thenReturn(true);
    }

    private DietaryPreferences dietaryPreferences() {
        DietaryPreferences preferences = new DietaryPreferences();
        preferences.noGluten = true;
        preferences.noLactose = true;
        preferences.noEggs = true;
        preferences.noFish = true;
        preferences.noChicken = true;
        preferences.noMeat = true;
        preferences.noNuts = true;
        return preferences;
    }

    private void assertDietaryPreferences(DietaryPreferences preferences) {
        assertAll(
                () -> assertTrue(preferences.noGluten),
                () -> assertTrue(preferences.noLactose),
                () -> assertTrue(preferences.noEggs),
                () -> assertTrue(preferences.noFish),
                () -> assertTrue(preferences.noChicken),
                () -> assertTrue(preferences.noMeat),
                () -> assertTrue(preferences.noNuts));
    }
}
