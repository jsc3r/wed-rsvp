package de.jsc3r.wedrsvp.invitation;

import de.jsc3r.wedrsvp.invitation.entity.DietaryPreferences;
import de.jsc3r.wedrsvp.invitation.entity.Guest;
import de.jsc3r.wedrsvp.invitation.entity.Invitation;
import de.jsc3r.wedrsvp.invitation.entity.Language;
import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;
import io.restassured.response.ValidatableResponse;
import jakarta.transaction.Transactional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;

@QuarkusTest
class InvitationIntegrationTest {

    private static final String INVITATION_CODE = "abc123";
    private static final String PIN = "123456";

    @BeforeEach
    @Transactional
    void resetDatabase() {
        Invitation.deleteAll();

        Invitation invitation = new Invitation();
        invitation.invitationCode = INVITATION_CODE;
        invitation.pin = PIN;
        invitation.language = Language.GERMAN;
        invitation.guests = new ArrayList<>();

        Guest guest = new Guest();
        guest.firstName = "Bilbo";
        guest.lastName = "Baggins";
        guest.accepted = true;
        guest.dietaryPreferences = dietaryPreferences();
        invitation.guests.add(guest);

        invitation.persist();
    }

    @Test
    void returnsDetailsForValidPin() {
        detailsRequest()
                .statusCode(200)
                .body("language", equalTo("GERMAN"))
                .body("guests[0].firstName", equalTo("Bilbo"))
                .body("guests[0].lastName", equalTo("Baggins"))
                .body("guests[0].accepted", equalTo(true))
                .body("guests[0].dietaryPreferences.noGluten", equalTo(true))
                .body("guests[0].dietaryPreferences.noNuts", equalTo(true));
    }

    @Test
    void rejectsDetailsForInvalidPin() {
        given()
                .queryParam("pin", "wrong")
                .when()
                .post("/api/invitation/{code}", INVITATION_CODE)
                .then()
                .statusCode(403);
    }

    @Test
    void rejectsDetailsForUnknownInvitation() {
        given()
                .queryParam("pin", PIN)
                .when()
                .post("/api/invitation/{code}", "unknown")
                .then()
                .statusCode(404);
    }

    @Test
    void acceptsInvitation() {
        given()
                .contentType(ContentType.JSON)
                .body("""
                        {
                          "guests": [
                            {
                              "accepted": false,
                              "dietaryPreferences": {
                                "noGluten": false,
                                "noLactose": true,
                                "noEggs": false,
                                "noFish": true,
                                "noChicken": false,
                                "noMeat": true,
                                "noNuts": false
                              }
                            }
                          ]
                        }
                        """)
                .when()
                .put("/api/invitation/{code}/accept", INVITATION_CODE)
                .then()
                .statusCode(202);

        detailsRequest()
                .body("guests.size()", equalTo(1))
                .body("guests[0].accepted", equalTo(false))
                .body("guests[0].dietaryPreferences.noLactose", equalTo(true))
                .body("guests[0].dietaryPreferences.noMeat", equalTo(true));
    }

    @Test
    void declinesInvitation() {
        given()
                .when()
                .put("/api/invitation/{code}/decline", INVITATION_CODE)
                .then()
                .statusCode(200);

        detailsRequest()
                .body("guests[0].accepted", equalTo(false));
    }

    @Test
    void returnsNotFoundWhenAcceptingUnknownInvitation() {
        given()
                .contentType(ContentType.JSON)
                .body("{\"guests\": [{\"accepted\": true, \"dietaryPreferences\": {\"noGluten\": false, \"noLactose\": false, \"noEggs\": false, \"noFish\": false, \"noChicken\": false, \"noMeat\": false, \"noNuts\": false}}]}")
                .when()
                .put("/api/invitation/{code}/accept", "unknown")
                .then()
                .statusCode(404);
    }

    @Test
    void validatesAcceptanceRequest() {
        given()
                .contentType(ContentType.JSON)
                .body("{\"guests\": []}")
                .when()
                .put("/api/invitation/{code}/accept", INVITATION_CODE)
                .then()
                .statusCode(400);
    }

    private ValidatableResponse detailsRequest() {
        return given()
                .queryParam("pin", PIN)
                .when()
                .post("/api/invitation/{code}", INVITATION_CODE)
                .then()
                .statusCode(200);
    }

    private DietaryPreferences dietaryPreferences() {
        DietaryPreferences preferences = new DietaryPreferences();
        preferences.noGluten = true;
        preferences.noLactose = false;
        preferences.noEggs = true;
        preferences.noFish = false;
        preferences.noChicken = true;
        preferences.noMeat = false;
        preferences.noNuts = true;
        return preferences;
    }
}
