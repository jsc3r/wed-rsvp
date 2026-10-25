package de.jsc3r.wedrsvp.invitation.entity;

import de.jsc3r.wedrsvp.auth.entity.UserAccount;
import io.quarkus.elytron.security.common.BcryptUtil;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.transaction.Transactional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@QuarkusTest
class InvitationEntityIntegrationTest {

    @BeforeEach
    @Transactional
    void resetDatabase() {
        Invitation.deleteAll();
    }

    @Test
    @Transactional
    void findsInvitationByCode() {
        var account = new UserAccount();
        account.roles.add("guest");
        account.persistAndFlush();
        var invitation = new Invitation();
        invitation.account = account;
        invitation.invitationCode = "abc123";
        invitation.pinHash = BcryptUtil.bcryptHash("0123", 12);
        invitation.language = Language.GERMAN;
        invitation.persistAndFlush();

        var result = Invitation.findByInvitationCode("abc123");

        assertTrue(result.isPresent());
        assertEquals(invitation.id, result.get().id);
        assertEquals("abc123", result.get().invitationCode);
        assertEquals(account.id, result.get().account.id);
    }

    @Test
    @Transactional
    void returnsEmptyForUnknownCode() {
        assertTrue(Invitation.findByInvitationCode("unknown").isEmpty());
    }
}
