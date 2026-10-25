package de.jsc3r.wedrsvp.invitation.entity;

import io.quarkus.hibernate.orm.panache.PanacheEntityBase;
import io.quarkus.hibernate.orm.panache.PanacheQuery;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class InvitationTest {

    @Mock
    PanacheQuery<Invitation> query;

    @Test
    void delegatesToPanacheFinder() {
        when(query.firstResultOptional()).thenReturn(Optional.empty());

        try (var panache = mockStatic(PanacheEntityBase.class)) {
            panache.when(() -> PanacheEntityBase.<Invitation>find("invitationCode", "unknown"))
                    .thenReturn(query);

            assertTrue(Invitation.findByInvitationCode("unknown").isEmpty());
        }
    }
}
