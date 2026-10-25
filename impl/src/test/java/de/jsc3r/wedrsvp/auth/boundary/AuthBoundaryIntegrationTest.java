package de.jsc3r.wedrsvp.auth.boundary;

import de.jsc3r.wedrsvp.auth.control.CredentialHasher;
import de.jsc3r.wedrsvp.auth.entity.LoginAttempts;
import de.jsc3r.wedrsvp.auth.entity.UserAccount;
import de.jsc3r.wedrsvp.invitation.entity.DietaryPreferences;
import de.jsc3r.wedrsvp.invitation.entity.Guest;
import de.jsc3r.wedrsvp.invitation.entity.Invitation;
import de.jsc3r.wedrsvp.invitation.entity.Language;
import io.quarkus.elytron.security.common.BcryptUtil;
import io.quarkus.narayana.jta.QuarkusTransaction;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.QuarkusTestProfile;
import io.quarkus.test.junit.TestProfile;
import io.quarkus.test.junit.mockito.InjectSpy;
import io.restassured.http.ContentType;
import io.restassured.path.json.JsonPath;
import io.restassured.response.Response;
import io.smallrye.jwt.build.Jwt;
import io.smallrye.jwt.build.JwtClaimsBuilder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Base64;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executors;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.anyOf;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@QuarkusTest
@TestProfile(AuthBoundaryIntegrationTest.Profile.class)
class AuthBoundaryIntegrationTest {

    private static final String OWN_CODE = "invitation-own-7zM4Xq2A";
    private static final String OTHER_CODE = "invitation-other-8yN5Wr3B";
    private static final String PIN = "0123";
    private static final String PIN_HASH = BcryptUtil.bcryptHash(PIN, 12);
    private static final String ADMIN_USERNAME = "wedding-admin";
    private static final String ADMIN_PASSWORD = "test-only-long-administrator-password";
    private static final String ACCOUNT_USERNAME = "invited-guest";
    private static final String ACCOUNT_PASSWORD = "test-only-invited-guest-password";
    private static final String ACCOUNT_PASSWORD_HASH = BcryptUtil.bcryptHash(ACCOUNT_PASSWORD, 12);
    private static final String ACCEPTANCE = """
            {"guests":[{"accepted":true,"dietaryPreferences":{
              "noGluten":false,"noLactose":false,"noEggs":false,"noFish":false,
              "noChicken":false,"noMeat":false,"noNuts":false}}]}
            """;

    @InjectSpy
    CredentialHasher hasher;

    private Long ownAccountId;

    public static class Profile implements QuarkusTestProfile {
        @Override
        public Map<String, String> getConfigOverrides() {
            return Map.of(
                    "app.auth.admin.username", ADMIN_USERNAME,
                    "app.auth.admin.password-hash", BcryptUtil.bcryptHash(ADMIN_PASSWORD, 12));
        }
    }

    @BeforeEach
    void resetDatabase() {
        ownAccountId = QuarkusTransaction.requiringNew().call(() -> {
            Invitation.deleteAll();
            for (UserAccount account : UserAccount.<UserAccount>listAll()) {
                if (!ADMIN_USERNAME.equals(account.username)) {
                    account.delete();
                }
            }
            var admin = UserAccount.findByUsername(ADMIN_USERNAME).orElseGet(() -> {
                var account = new UserAccount();
                account.username = ADMIN_USERNAME;
                account.passwordHash = BcryptUtil.bcryptHash(ADMIN_PASSWORD, 12);
                account.persistAndFlush();
                return account;
            });
            admin.loginAttempts = new LoginAttempts();
            admin.roles = new HashSet<>(Set.of("admin"));
            var own = persistInvitation(OWN_CODE);
            own.account.username = ACCOUNT_USERNAME;
            own.account.passwordHash = ACCOUNT_PASSWORD_HASH;
            persistInvitation(OTHER_CODE);
            return own.account.id;
        });
    }

    @Test
    void guestReceivesShortLivedTokenWithOnlyOwnAccessRights() {
        var response = pinLogin(OWN_CODE, PIN);
        response.then().statusCode(200)
                .header("Cache-Control", equalTo("no-store"))
                .body("tokenType", equalTo("Bearer"))
                .body("expiresIn", equalTo(900))
                .body("roles", contains("guest"))
                .body("accessToken", not(nullValue()));

        var token = response.jsonPath().getString("accessToken");
        var claims = claims(token);
        assertEquals("user:" + ownAccountId, claims.getString("sub"));
        assertEquals(OWN_CODE, claims.getString("invitation_code"));
        assertEquals("wed-rsvp", claims.getString("iss"));
        assertEquals(900L, claims.getLong("exp") - claims.getLong("iat"));
        assertFalse(claims.getMap("$").containsKey("pin"));
        assertFalse(claims.getMap("$").containsKey("pinHash"));

        given().auth().oauth2(token).when().get("/api/auth/me")
                .then().statusCode(200)
                .body("subject", equalTo("user:" + ownAccountId))
                .body("roles", contains("guest"))
                .body("invitationCode", equalTo(OWN_CODE));
    }

    @Test
    void wrongPinAndUnknownInvitationHaveTheSameResponse() {
        var wrongPin = pinLogin(OWN_CODE, "9999");
        var unknownCode = pinLogin("unknown-invitation", "9999");

        wrongPin.then().statusCode(401);
        unknownCode.then().statusCode(401);
        assertEquals(wrongPin.asString(), unknownCode.asString());
        assertFalse(wrongPin.asString().contains(OWN_CODE));
        assertFalse(wrongPin.asString().contains("9999"));
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "123", "12345", "abcd", "01 3", "０１２３", "٠١٢٣"})
    void rejectsPinsThatAreNotFourAsciiDigits(String pin) {
        pinLogin(OWN_CODE, pin).then().statusCode(400);
    }

    @Test
    void rejectsMissingCredentialsAndMalformedJson() {
        for (String body : List.of("{}", "{\"invitationCode\":\"" + OWN_CODE + "\"}",
                "{\"invitationCode\":\"\",\"pin\":\"0123\"}", "null", "{")) {
            given().contentType(ContentType.JSON).body(body)
                    .when().post("/api/auth/login/pin").then().statusCode(400);
        }
    }

    @Test
    void guestCanReadAndUpdateOwnInvitation() {
        var token = guestToken();
        given().auth().oauth2(token).when().get(invitationPath(OWN_CODE))
                .then().statusCode(200);

        given().auth().oauth2(token).contentType(ContentType.JSON).body(ACCEPTANCE)
                .when().put(invitationPath(OWN_CODE) + "/accept").then().statusCode(202);
        assertTrue(isAccepted(OWN_CODE));

        given().auth().oauth2(token).when().put(invitationPath(OWN_CODE) + "/decline")
                .then().statusCode(200);
        assertFalse(isAccepted(OWN_CODE));
    }

    @Test
    void credentialsLoginUsesStoredGuestRolesAndInvitationScope() {
        var response = credentialsLogin(ACCOUNT_USERNAME, ACCOUNT_PASSWORD);
        response.then().statusCode(200).body("roles", contains("guest"));
        var token = response.jsonPath().getString("accessToken");
        assertEquals("user:" + ownAccountId, claims(token).getString("sub"));
        assertEquals(OWN_CODE, claims(token).getString("invitation_code"));

        given().auth().oauth2(token).when().get("/api/test-security/admin")
                .then().statusCode(403);
        given().auth().oauth2(token).contentType(ContentType.JSON).body(ACCEPTANCE)
                .when().put(invitationPath(OWN_CODE) + "/accept").then().statusCode(202);
        assertTrue(isAccepted(OWN_CODE));
        given().auth().oauth2(token).when().put(invitationPath(OWN_CODE) + "/decline")
                .then().statusCode(200);
        assertFalse(isAccepted(OWN_CODE));
        given().auth().oauth2(token).contentType(ContentType.JSON).body(ACCEPTANCE)
                .when().put(invitationPath(OTHER_CODE) + "/accept").then().statusCode(403);
        given().auth().oauth2(token).when().put(invitationPath(OTHER_CODE) + "/decline")
                .then().statusCode(403);
        assertFalse(isAccepted(OTHER_CODE));
    }

    @Test
    void pinLoginUsesStoredAdminRole() {
        setOwnRoles(Set.of("admin"));

        var response = pinLogin(OWN_CODE, PIN);
        response.then().statusCode(200).body("roles", contains("admin"));
        var token = response.jsonPath().getString("accessToken");
        given().auth().oauth2(token).when().get("/api/auth/me")
                .then().statusCode(200).body("invitationCode", equalTo(OWN_CODE));
        given().auth().oauth2(token).when().get("/api/test-security/admin")
                .then().statusCode(200);
        given().auth().oauth2(token).contentType(ContentType.JSON).body(ACCEPTANCE)
                .when().put(invitationPath(OTHER_CODE) + "/accept").then().statusCode(202);
        assertTrue(isAccepted(OTHER_CODE));
    }

    @Test
    void bothLoginMethodsPreserveEveryStoredRole() {
        setOwnRoles(Set.of("guest", "admin"));

        for (var response : List.of(pinLogin(OWN_CODE, PIN),
                credentialsLogin(ACCOUNT_USERNAME, ACCOUNT_PASSWORD))) {
            response.then().statusCode(200).body("roles", containsInAnyOrder("guest", "admin"));
            var token = response.jsonPath().getString("accessToken");
            assertEquals(Set.of("guest", "admin"), Set.copyOf(claims(token).getList("groups")));
            given().auth().oauth2(token).when().get("/api/test-security/admin")
                    .then().statusCode(200);
        }
    }

    @Test
    void authenticatedAccountWithoutRolesCannotAccessProtectedEndpoints() {
        setOwnRoles(Set.of());

        for (var response : List.of(pinLogin(OWN_CODE, PIN),
                credentialsLogin(ACCOUNT_USERNAME, ACCOUNT_PASSWORD))) {
            response.then().statusCode(200).body("roles", empty());
            var token = response.jsonPath().getString("accessToken");
            given().auth().oauth2(token).when().get("/api/auth/me").then().statusCode(403);
            given().auth().oauth2(token).when().get("/api/test-security/admin").then().statusCode(403);
            given().auth().oauth2(token).when().get(invitationPath(OWN_CODE)).then().statusCode(403);
            given().auth().oauth2(token).contentType(ContentType.JSON).body(ACCEPTANCE)
                    .when().put(invitationPath(OWN_CODE) + "/accept").then().statusCode(403);
            given().auth().oauth2(token).when().put(invitationPath(OWN_CODE) + "/decline")
                    .then().statusCode(403);
        }
        assertFalse(isAccepted(OWN_CODE));
    }

    @Test
    void loginRequestsCannotGrantRoles() {
        var responses = List.of(
                given().contentType(ContentType.JSON)
                        .body(Map.of("invitationCode", OWN_CODE, "pin", PIN, "roles", List.of("admin")))
                        .when().post("/api/auth/login/pin"),
                given().contentType(ContentType.JSON)
                        .body(Map.of("username", ACCOUNT_USERNAME, "password", ACCOUNT_PASSWORD,
                                "roles", List.of("admin")))
                        .when().post("/api/auth/login/credentials"));

        for (var response : responses) {
            response.then().statusCode(200).body("roles", contains("guest"));
            var token = response.jsonPath().getString("accessToken");
            given().auth().oauth2(token).when().get("/api/test-security/admin").then().statusCode(403);
        }
    }

    @Test
    void invitationWithoutAccountCannotAuthenticateWithPin() {
        QuarkusTransaction.requiringNew().run(() -> Invitation.findByInvitationCode(OWN_CODE)
                .orElseThrow().account = null);

        pinLogin(OWN_CODE, PIN).then().statusCode(401);
    }

    @Test
    void guestAccountWithoutInvitationCannotAccessAnyInvitation() {
        QuarkusTransaction.requiringNew().run(() -> Invitation.findByInvitationCode(OWN_CODE)
                .orElseThrow().account = null);

        var token = credentialsLogin(ACCOUNT_USERNAME, ACCOUNT_PASSWORD)
                .then().statusCode(200).body("roles", contains("guest"))
                .extract().<String>path("accessToken");
        given().auth().oauth2(token).when().get("/api/auth/me")
                .then().statusCode(200).body("invitationCode", nullValue());
        given().auth().oauth2(token).when().get(invitationPath(OWN_CODE))
                .then().statusCode(403);
    }

    @Test
    void guestCannotReadOrChangeAnotherInvitation() {
        var token = guestToken();
        given().auth().oauth2(token).when().get(invitationPath(OTHER_CODE))
                .then().statusCode(403);
        given().auth().oauth2(token).contentType(ContentType.JSON).body(ACCEPTANCE)
                .when().put(invitationPath(OTHER_CODE) + "/accept").then().statusCode(403);
        given().auth().oauth2(token).when().put(invitationPath(OTHER_CODE) + "/decline")
                .then().statusCode(403);
        assertFalse(isAccepted(OTHER_CODE));
    }

    @Test
    void invitationPermissionIsCheckedBeforeLookingUpTheInvitation() {
        var token = guestToken();
        var path = invitationPath("nonexistent-invitation");

        given().auth().oauth2(token).when().get(path).then().statusCode(403);
        given().auth().oauth2(token).contentType(ContentType.JSON).body(ACCEPTANCE)
                .when().put(path + "/accept").then().statusCode(403);
        given().auth().oauth2(token).when().put(path + "/decline").then().statusCode(403);
    }

    @Test
    void invitationEndpointsRequireAuthentication() {
        given().when().get(invitationPath(OWN_CODE)).then().statusCode(401);
        given().contentType(ContentType.JSON).body(ACCEPTANCE)
                .when().put(invitationPath(OWN_CODE) + "/accept").then().statusCode(401);
        given().when().put(invitationPath(OWN_CODE) + "/decline").then().statusCode(401);
        given().when().get("/api/auth/me").then().statusCode(401);
    }

    @Test
    void guestFailuresPersistAndLockTheInvitationUntilExpiry() {
        for (var failures = 1; failures <= 5; failures++) {
            pinLogin(OWN_CODE, "9999").then().statusCode(401);
            var expectedFailures = failures;
            QuarkusTransaction.requiringNew().run(() -> assertEquals(expectedFailures,
                    Invitation.findByInvitationCode(OWN_CODE).orElseThrow().loginAttempts.failures));
        }
        QuarkusTransaction.requiringNew().run(() -> {
            var lockedUntil = Invitation.findByInvitationCode(OWN_CODE).orElseThrow()
                    .loginAttempts.lockedUntil;
            assertNotNull(lockedUntil);
            assertTrue(lockedUntil.isAfter(Instant.now().plusSeconds(14 * 60)));
            assertTrue(lockedUntil.isBefore(Instant.now().plusSeconds(16 * 60)));
        });

        pinLogin(OWN_CODE, PIN).then().statusCode(401);
        pinLogin(OTHER_CODE, PIN).then().statusCode(200);
        QuarkusTransaction.requiringNew().run(() -> Invitation.findByInvitationCode(OWN_CODE)
                .orElseThrow().loginAttempts.lockedUntil = Instant.now().minusSeconds(1));

        pinLogin(OWN_CODE, PIN).then().statusCode(200);
        QuarkusTransaction.requiringNew().run(() -> {
            var attempts = Invitation.findByInvitationCode(OWN_CODE).orElseThrow().loginAttempts;
            assertEquals(0, attempts.failures);
            assertNull(attempts.lockedUntil);
        });
    }

    @Test
    void successfulGuestLoginResetsPreviousFailures() {
        pinLogin(OWN_CODE, "9999").then().statusCode(401);
        pinLogin(OWN_CODE, PIN).then().statusCode(200);
        QuarkusTransaction.requiringNew().run(() -> assertEquals(0,
                Invitation.findByInvitationCode(OWN_CODE).orElseThrow().loginAttempts.failures));
    }

    @ParameterizedTest
    @ValueSource(strings = {PIN, "9999"})
    void lockedAndUnknownInvitationsBothPerformDummyVerification(String pin) {
        var lockedUntil = Instant.now().plusSeconds(900).truncatedTo(ChronoUnit.SECONDS);
        QuarkusTransaction.requiringNew().run(() -> {
            var attempts = Invitation.findByInvitationCode(OWN_CODE).orElseThrow().loginAttempts;
            attempts.failures = 5;
            attempts.lockedUntil = lockedUntil;
        });

        var locked = pinLogin(OWN_CODE, pin);
        var unknown = pinLogin("unknown-invitation", pin);

        locked.then().statusCode(401);
        unknown.then().statusCode(401);
        assertEquals(unknown.asString(), locked.asString());
        verify(hasher, times(2)).matches(pin, null);
        QuarkusTransaction.requiringNew().run(() -> {
            var attempts = Invitation.findByInvitationCode(OWN_CODE).orElseThrow().loginAttempts;
            assertEquals(5, attempts.failures);
            assertEquals(lockedUntil, attempts.lockedUntil);
        });
    }

    @Test
    void concurrentFailedLoginsCannotBypassTheAttemptLimit() {
        try (var executor = Executors.newFixedThreadPool(5)) {
            List<CompletableFuture<Integer>> attempts = new ArrayList<>();
            for (int attempt = 0; attempt < 5; attempt++) {
                attempts.add(CompletableFuture.supplyAsync(
                        () -> pinLogin(OWN_CODE, "9999").statusCode(), executor));
            }
            for (var attempt : attempts) {
                assertEquals(401, attempt.join());
            }
        }
        for (int attempt = 0; attempt < 3; attempt++) {
            pinLogin(OWN_CODE, PIN).then().statusCode(401);
        }
        QuarkusTransaction.requiringNew().run(() -> {
            var attempts = Invitation.findByInvitationCode(OWN_CODE).orElseThrow().loginAttempts;
            assertEquals(5, attempts.failures);
            assertNotNull(attempts.lockedUntil);
        });
    }

    @Test
    void legacyPlaintextPinCannotAuthenticate() {
        QuarkusTransaction.requiringNew().run(() -> Invitation.findByInvitationCode(OWN_CODE)
                .orElseThrow().pinHash = PIN);

        pinLogin(OWN_CODE, PIN).then().statusCode(401);
    }

    @Test
    void adminReceivesAdminRoleAndCanAccessEveryInvitation() {
        var response = credentialsLogin(ADMIN_USERNAME, ADMIN_PASSWORD);
        response.then().statusCode(200)
                .header("Cache-Control", equalTo("no-store"))
                .body("tokenType", equalTo("Bearer"))
                .body("expiresIn", equalTo(900))
                .body("roles", contains("admin"));
        var token = response.jsonPath().getString("accessToken");

        given().auth().oauth2(token).when().get("/api/auth/me")
                .then().statusCode(200)
                .body("roles", contains("admin"))
                .body("invitationCode", nullValue());
        given().auth().oauth2(token).when().get(invitationPath(OTHER_CODE))
                .then().statusCode(200);
        given().auth().oauth2(token).contentType(ContentType.JSON).body(ACCEPTANCE)
                .when().put(invitationPath(OTHER_CODE) + "/accept").then().statusCode(202);
        assertTrue(isAccepted(OTHER_CODE));
        given().auth().oauth2(token).when().put(invitationPath(OTHER_CODE) + "/decline")
                .then().statusCode(200);
        assertFalse(isAccepted(OTHER_CODE));
    }

    @Test
    void adminFailuresPersistAndLockEvenWithCorrectPassword() {
        var unknown = credentialsLogin("unknown-admin", ADMIN_PASSWORD);
        var wrong = credentialsLogin(ADMIN_USERNAME, "wrong-password");
        unknown.then().statusCode(401);
        wrong.then().statusCode(401);
        assertEquals(unknown.asString(), wrong.asString());
        QuarkusTransaction.requiringNew().run(() -> assertEquals(1,
                UserAccount.findByUsername(ADMIN_USERNAME).orElseThrow().loginAttempts.failures));
        for (var attempt = 1; attempt < 5; attempt++) {
            credentialsLogin(ADMIN_USERNAME, "wrong-password").then().statusCode(401);
        }
        QuarkusTransaction.requiringNew().run(() -> {
            var state = UserAccount.findByUsername(ADMIN_USERNAME).orElseThrow();
            assertEquals(5, state.loginAttempts.failures);
            assertNotNull(state.loginAttempts.lockedUntil);
        });
        credentialsLogin(ADMIN_USERNAME, ADMIN_PASSWORD).then().statusCode(401);

        QuarkusTransaction.requiringNew().run(() -> {
            var state = UserAccount.findByUsername(ADMIN_USERNAME).orElseThrow();
            state.loginAttempts.lockedUntil = Instant.now().minusSeconds(1);
        });
        credentialsLogin(ADMIN_USERNAME, ADMIN_PASSWORD).then().statusCode(200);
        QuarkusTransaction.requiringNew().run(() -> {
            var state = UserAccount.findByUsername(ADMIN_USERNAME).orElseThrow();
            assertEquals(0, state.loginAttempts.failures);
            assertNull(state.loginAttempts.lockedUntil);
        });
    }

    @Test
    void oversizedUtf8AdminPasswordIsRejectedWithoutServerError() {
        credentialsLogin(ADMIN_USERNAME, "é".repeat(72)).then()
                .statusCode(anyOf(equalTo(400), equalTo(401)));
    }

    @ParameterizedTest
    @ValueSource(strings = {ACCOUNT_PASSWORD, "wrong-password"})
    void lockedAndUnknownAccountsBothPerformDummyVerification(String password) {
        var lockedUntil = Instant.now().plusSeconds(900).truncatedTo(ChronoUnit.SECONDS);
        QuarkusTransaction.requiringNew().run(() -> {
            LoginAttempts attempts = UserAccount.findByUsername(ACCOUNT_USERNAME).orElseThrow().loginAttempts;
            attempts.failures = 5;
            attempts.lockedUntil = lockedUntil;
        });

        var locked = credentialsLogin(ACCOUNT_USERNAME, password);
        var unknown = credentialsLogin("unknown-user", password);

        locked.then().statusCode(401);
        unknown.then().statusCode(401);
        assertEquals(unknown.asString(), locked.asString());
        verify(hasher, times(2)).matches(password, null);
        QuarkusTransaction.requiringNew().run(() -> {
            var attempts = UserAccount.findByUsername(ACCOUNT_USERNAME).orElseThrow().loginAttempts;
            assertEquals(5, attempts.failures);
            assertEquals(lockedUntil, attempts.lockedUntil);
        });
    }

    @Test
    void successfulCredentialsLoginResetsOnlyItsOwnFailures() {
        credentialsLogin(ADMIN_USERNAME, "wrong-password").then().statusCode(401);
        credentialsLogin(ACCOUNT_USERNAME, "wrong-password").then().statusCode(401);
        credentialsLogin(ACCOUNT_USERNAME, ACCOUNT_PASSWORD).then().statusCode(200);

        QuarkusTransaction.requiringNew().run(() -> {
            assertEquals(0, UserAccount.findByUsername(ACCOUNT_USERNAME).orElseThrow().loginAttempts.failures);
            assertEquals(1, UserAccount.findByUsername(ADMIN_USERNAME).orElseThrow().loginAttempts.failures);
        });
    }

    @Test
    void rejectsMissingCredentialsForPasswordLogin() {
        for (var body : List.of("{}", "{\"username\":\"user\"}",
                "{\"username\":\"\",\"password\":\"password\"}", "null", "{")) {
            given().contentType(ContentType.JSON).body(body)
                    .when().post("/api/auth/login/credentials").then().statusCode(400);
        }
    }

    @Test
    void rejectsTamperedAndMalformedTokens() {
        var token = guestToken();
        var signatureStart = token.lastIndexOf('.') + 1;
        var replacement = token.charAt(signatureStart) == 'A' ? 'B' : 'A';
        var tampered = token.substring(0, signatureStart) + replacement + token.substring(signatureStart + 1);
        for (var invalidToken : List.of(tampered, "not-a-jwt")) {
            given().auth().oauth2(invalidToken).when().get("/api/auth/me")
                    .then().statusCode(401);
        }
    }

    @Test
    void rejectsExpiredTokensAndTokensForAnotherIssuerOrAudience() {
        var now = Instant.now().getEpochSecond();
        var invalidTokens = List.of(
                guestClaims().issuedAt(now - 100).expiresAt(now - 30).sign(),
                guestClaims().issuer("other-issuer").sign(),
                guestClaims().audience("other-api").sign());
        for (var token : invalidTokens) {
            given().auth().oauth2(token).when().get(invitationPath(OWN_CODE))
                    .then().statusCode(401);
        }
    }

    @Test
    void guestTokenWithoutInvitationScopeCannotAccessInvitations() {
        var token = Jwt.issuer("wed-rsvp").audience("wed-rsvp-api")
                .subject("user:" + ownAccountId).groups(Set.of("guest"))
                .expiresIn(900).sign();
        given().auth().oauth2(token).when().get(invitationPath(OWN_CODE))
                .then().statusCode(403);
    }

    @Test
    void guestTokenForADifferentIdentityCannotReuseAnInvitationCode() {
        var token = guestClaims().subject("user:" + (ownAccountId + 999)).sign();

        given().auth().oauth2(token).when().get(invitationPath(OWN_CODE))
                .then().statusCode(403);
    }

    @Test
    void previousAccountCannotUseTokenAfterInvitationOwnershipChanges() {
        var token = guestToken();
        QuarkusTransaction.requiringNew().run(() -> {
            var replacement = new UserAccount();
            replacement.roles.add("guest");
            replacement.persistAndFlush();
            Invitation.findByInvitationCode(OWN_CODE).orElseThrow().account = replacement;
        });

        given().auth().oauth2(token).when().get(invitationPath(OWN_CODE))
                .then().statusCode(403);
        given().auth().oauth2(token).contentType(ContentType.JSON).body(ACCEPTANCE)
                .when().put(invitationPath(OWN_CODE) + "/accept").then().statusCode(403);
        given().auth().oauth2(token).when().put(invitationPath(OWN_CODE) + "/decline")
                .then().statusCode(403);
        assertFalse(isAccepted(OWN_CODE));
    }

    @Test
    void tokenWithUnrecognizedRoleCannotAccessProtectedResources() {
        var token = guestClaims().groups(Set.of("unknown")).sign();
        given().auth().oauth2(token).when().get(invitationPath(OWN_CODE))
                .then().statusCode(403);
        given().auth().oauth2(token).when().get("/api/auth/me")
                .then().statusCode(403);
    }

    @Test
    void adminResourceEnforcesRolesAndUnannotatedResourceIsDeniedByDefault() {
        var guestToken = guestToken();
        String adminToken = credentialsLogin(ADMIN_USERNAME, ADMIN_PASSWORD)
                .then().statusCode(200).extract().path("accessToken");

        given().when().get("/api/test-security/admin").then().statusCode(401);
        given().auth().oauth2(guestToken).when().get("/api/test-security/admin")
                .then().statusCode(403);
        given().auth().oauth2(adminToken).when().get("/api/test-security/admin")
                .then().statusCode(200);
        given().when().get("/api/test-security/unannotated").then().statusCode(401);
        given().auth().oauth2(adminToken).when().get("/api/test-security/unannotated")
                .then().statusCode(403);
    }

    private static Invitation persistInvitation(String code) {
        var account = new UserAccount();
        account.roles.add("guest");
        account.persistAndFlush();
        var invitation = new Invitation();
        invitation.account = account;
        invitation.invitationCode = code;
        invitation.pinHash = PIN_HASH;
        invitation.language = Language.GERMAN;
        Guest guest = new Guest();
        guest.firstName = "Test";
        guest.lastName = "Guest";
        guest.dietaryPreferences = new DietaryPreferences();
        invitation.guests.add(guest);
        invitation.persistAndFlush();
        return invitation;
    }

    private static boolean isAccepted(String code) {
        return QuarkusTransaction.requiringNew().call(() -> Invitation.findByInvitationCode(code)
                .orElseThrow().guests.getFirst().accepted);
    }

    private static void setOwnRoles(Set<String> roles) {
        QuarkusTransaction.requiringNew().run(() -> UserAccount.findByUsername(ACCOUNT_USERNAME)
                .orElseThrow().roles = new HashSet<>(roles));
    }

    private static Response pinLogin(String code, String pin) {
        return given().contentType(ContentType.JSON).body(Map.of("invitationCode", code, "pin", pin))
                .when().post("/api/auth/login/pin");
    }

    private static Response credentialsLogin(String username, String password) {
        return given().contentType(ContentType.JSON).body(Map.of("username", username, "password", password))
                .when().post("/api/auth/login/credentials");
    }

    private static String guestToken() {
        return pinLogin(OWN_CODE, PIN).then().statusCode(200).extract().path("accessToken");
    }

    private JwtClaimsBuilder guestClaims() {
        return Jwt.issuer("wed-rsvp").audience("wed-rsvp-api")
                .subject("user:" + ownAccountId).groups(Set.of("guest"))
                .claim("invitation_code", OWN_CODE).expiresIn(900);
    }

    private static JsonPath claims(String token) {
        return JsonPath.from(new String(Base64.getUrlDecoder().decode(token.split("\\.")[1]),
                StandardCharsets.UTF_8));
    }

    private static String invitationPath(String code) {
        return "/api/invitation/" + code;
    }
}
