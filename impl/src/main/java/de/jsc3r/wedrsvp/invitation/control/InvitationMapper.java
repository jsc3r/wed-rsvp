package de.jsc3r.wedrsvp.invitation.control;

import de.jsc3r.wedrsvp.api.invitation.dto.request.GuestAcceptanceRequest;
import de.jsc3r.wedrsvp.api.invitation.dto.response.InvitationDetails;
import de.jsc3r.wedrsvp.invitation.entity.DietaryPreferences;
import de.jsc3r.wedrsvp.invitation.entity.Guest;
import de.jsc3r.wedrsvp.invitation.entity.Invitation;
import jakarta.enterprise.context.ApplicationScoped;

import java.util.List;

@ApplicationScoped
public class InvitationMapper {

    Guest toGuest(GuestAcceptanceRequest request) {
        Guest guest = new Guest();
        guest.accepted = request.accepted();
        guest.dietaryPreferences = toDiet(request.dietaryPreferences());
        return guest;
    }

    InvitationDetails toInvitationDetails(Invitation invitation) {
        List<de.jsc3r.wedrsvp.api.invitation.dto.response.Guest> guests = invitation.guests.stream()
                .map(this::toGuestDetails)
                .toList();
        return new InvitationDetails(
                guests,
                de.jsc3r.wedrsvp.api.invitation.dto.response.Language.valueOf(invitation.language.name()));
    }

    private de.jsc3r.wedrsvp.api.invitation.dto.response.Guest toGuestDetails(Guest guest) {
        de.jsc3r.wedrsvp.api.invitation.dto.response.Guest guestDetails =
                new de.jsc3r.wedrsvp.api.invitation.dto.response.Guest();
        guestDetails.firstName = guest.firstName;
        guestDetails.lastName = guest.lastName;
        guestDetails.accepted = guest.accepted;
        guestDetails.dietaryPreferences = new de.jsc3r.wedrsvp.api.invitation.dto.response.DietaryPreferences(
                guest.dietaryPreferences.noGluten,
                guest.dietaryPreferences.noLactose,
                guest.dietaryPreferences.noEggs,
                guest.dietaryPreferences.noFish,
                guest.dietaryPreferences.noChicken,
                guest.dietaryPreferences.noMeat,
                guest.dietaryPreferences.noNuts);
        return guestDetails;
    }

    private DietaryPreferences toDiet(de.jsc3r.wedrsvp.api.invitation.dto.DietaryPreferences preferences) {
        DietaryPreferences dietaryPreferences = new DietaryPreferences();
        dietaryPreferences.noGluten = preferences.noGluten();
        dietaryPreferences.noLactose = preferences.noLactose();
        dietaryPreferences.noEggs = preferences.noEggs();
        dietaryPreferences.noFish = preferences.noFish();
        dietaryPreferences.noChicken = preferences.noChicken();
        dietaryPreferences.noMeat = preferences.noMeat();
        dietaryPreferences.noNuts = preferences.noNuts();
        return dietaryPreferences;
    }
}
