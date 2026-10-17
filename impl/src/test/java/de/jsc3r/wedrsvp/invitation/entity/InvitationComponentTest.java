package de.jsc3r.wedrsvp.invitation.entity;

import io.quarkus.hibernate.orm.panache.PanacheEntityBase;
import io.quarkus.hibernate.orm.panache.PanacheQuery;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.transaction.Transactional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.*;

@QuarkusTest
class InvitationComponentTest {

    @BeforeEach
    @Transactional
    void resetDatabase() {
        Invitation.deleteAll();
    }

    @Test
    @Transactional
    void findsInvitationByCode() {
        Invitation invitation = new Invitation();
        invitation.invitationCode = "abc123";
        invitation.pin = "123456";
        invitation.language = Language.GERMAN;
        invitation.persistAndFlush();

        Optional<Invitation> result = Invitation.findByInvitationCode("abc123");

        assertTrue(result.isPresent());
        assertEquals(invitation.id, result.get().id);
        assertEquals("abc123", result.get().invitationCode);
    }

    @Test
    @Transactional
    void returnsEmptyForUnknownCode() {
        assertTrue(Invitation.findByInvitationCode("unknown").isEmpty());
    }

    @Test
    @SuppressWarnings("unchecked")
    void delegatesToPanacheFinder() {
        PanacheQuery<Invitation> query = mock(PanacheQuery.class);
        when(query.firstResultOptional()).thenReturn(Optional.empty());

        try (var panache = mockStatic(PanacheEntityBase.class)) {
            panache.when(() -> PanacheEntityBase.<Invitation>find("invitationCode", "unknown"))
                    .thenReturn(query);

            assertTrue(Invitation.findByInvitationCode("unknown").isEmpty());
        }
    }
}
