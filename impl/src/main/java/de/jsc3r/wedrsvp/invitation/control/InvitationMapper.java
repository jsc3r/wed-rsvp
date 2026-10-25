package de.jsc3r.wedrsvp.invitation.control;

import de.jsc3r.wedrsvp.api.invitation.dto.request.GuestAcceptanceRequest;
import de.jsc3r.wedrsvp.api.invitation.dto.response.InvitationDetails;
import de.jsc3r.wedrsvp.invitation.entity.DietaryPreferences;
import de.jsc3r.wedrsvp.invitation.entity.Guest;
import de.jsc3r.wedrsvp.invitation.entity.Invitation;
import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class InvitationMapper {

    Guest toGuest(GuestAcceptanceRequest request) {
        var guest = new Guest();
        guest.accepted = request.accepted();
        guest.dietaryPreferences = toDiet(request.dietaryPreferences());
        return guest;
    }

    InvitationDetails toInvitationDetails(Invitation invitation) {
        var guests = invitation.guests.stream()
                .map(this::toGuestDetails)
                .toList();
        return new InvitationDetails(
                guests,
                de.jsc3r.wedrsvp.api.invitation.dto.response.Language.valueOf(invitation.language.name()));
    }

    private de.jsc3r.wedrsvp.api.invitation.dto.response.Guest toGuestDetails(Guest guest) {
        var guestDetails = new de.jsc3r.wedrsvp.api.invitation.dto.response.Guest();
        guestDetails.firstName = guest.firstName;
        guestDetails.lastName = guest.lastName;
        guestDetails.accepted = guest.accepted;
        guestDetails.dietaryPreferences = new de.jsc3r.wedrsvp.api.invitation.dto.DietaryPreferences(
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
        var dietaryPreferences = new DietaryPreferences();
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
