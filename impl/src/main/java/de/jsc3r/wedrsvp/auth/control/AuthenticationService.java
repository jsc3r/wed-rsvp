package de.jsc3r.wedrsvp.auth.control;

import de.jsc3r.wedrsvp.api.auth.dto.request.CredentialsLoginRequest;
import de.jsc3r.wedrsvp.api.auth.dto.request.PinLoginRequest;
import de.jsc3r.wedrsvp.api.auth.dto.response.TokenResponse;
import de.jsc3r.wedrsvp.auth.entity.LoginAttempts;
import de.jsc3r.wedrsvp.auth.entity.UserAccount;
import de.jsc3r.wedrsvp.invitation.entity.Invitation;
import io.quarkus.runtime.StartupEvent;
import io.smallrye.jwt.build.Jwt;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import jakarta.inject.Inject;
import jakarta.persistence.LockModeType;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.NotAuthorizedException;
import org.eclipse.microprofile.config.inject.ConfigProperty;

import java.time.Instant;
import java.util.Set;

@ApplicationScoped
public class AuthenticationService {

    private static final long TOKEN_LIFETIME_SECONDS = 900;
    private static final String UNSET = "__unset__";

    @Inject
    CredentialHasher hasher;

    @ConfigProperty(name = "app.auth.admin.username")
    String username;

    @ConfigProperty(name = "app.auth.admin.password-hash")
    String password;

    @SuppressWarnings("unused")
    void initialize(@Observes StartupEvent event) {
        initialize();
    }

    @Transactional
    void initialize() {
        var usernameUnset = username == null || username.isEmpty() || UNSET.equals(username);
        var passwordUnset = password == null || password.isEmpty() || UNSET.equals(password);
        if (usernameUnset && passwordUnset) {
            return;
        }
        if (usernameUnset || passwordUnset) {
            throw new IllegalStateException("Configure both admin username and password hash");
        }
        if (username.isBlank() || username.length() > 128) {
            throw new IllegalStateException("Admin username must be nonblank and at most 128 characters");
        }
        if (!hasher.isValidHash(password)) {
            throw new IllegalStateException("Admin password must be a bcrypt hash with cost 12 to 16");
        }
        if (UserAccount.findByUsername(username).isPresent()) {
            return;
        }
        var account = new UserAccount();
        account.username = username;
        account.passwordHash = password;
        account.roles.add("admin");
        account.persist();
    }

    // Authentication failures must commit their counters, including failed HTTP requests.
    @Transactional(dontRollbackOn = NotAuthorizedException.class)
    public TokenResponse login(PinLoginRequest request) {
        var result = Invitation.<Invitation>find("invitationCode", request.invitationCode())
                .withLock(LockModeType.PESSIMISTIC_WRITE).firstResultOptional();
        if (result.isEmpty() || result.get().account == null) {
            hasher.matches(request.pin(), null);
            throw unauthorized();
        }
        var invitation = result.get();
        verify(request.pin(), invitation.pinHash, invitation.loginAttempts);
        return token(invitation.account, invitation);
    }

    @Transactional(dontRollbackOn = NotAuthorizedException.class)
    public TokenResponse login(CredentialsLoginRequest request) {
        var result = UserAccount.<UserAccount>find("username", request.username())
                .withLock(LockModeType.PESSIMISTIC_WRITE).firstResultOptional();
        if (result.isEmpty()) {
            hasher.matches(request.password(), null);
            throw unauthorized();
        }
        var account = result.get();
        verify(request.password(), account.passwordHash, account.loginAttempts);
        return token(account, account.invitation);
    }

    private TokenResponse token(UserAccount account, Invitation invitation) {
        var roles = Set.copyOf(account.roles);
        var claims = Jwt.subject("user:" + account.id).groups(roles).expiresIn(TOKEN_LIFETIME_SECONDS);
        if (invitation != null) {
            claims.claim("invitation_code", invitation.invitationCode);
        }
        return new TokenResponse(claims.sign(), "Bearer", TOKEN_LIFETIME_SECONDS, roles);
    }

    private void verify(String credential, String hash, LoginAttempts attempts) {
        var now = Instant.now();
        attempts.resetExpiredLock(now);
        if (attempts.isLocked(now)) {
            // Locked and unknown accounts must both pay the dummy verification cost.
            hasher.matches(credential, null);
            throw unauthorized();
        }
        if (!hasher.matches(credential, hash)) {
            attempts.recordFailure(now);
            throw unauthorized();
        }
        attempts.reset();
    }

    private NotAuthorizedException unauthorized() {
        return new NotAuthorizedException("Invalid credentials", "Bearer");
    }
}
