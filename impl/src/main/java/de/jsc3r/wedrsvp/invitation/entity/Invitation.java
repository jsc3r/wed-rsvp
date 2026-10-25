package de.jsc3r.wedrsvp.invitation.entity;

import io.quarkus.hibernate.orm.panache.PanacheEntity;
import de.jsc3r.wedrsvp.auth.entity.LoginAttempts;
import de.jsc3r.wedrsvp.auth.entity.UserAccount;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Embedded;
import jakarta.persistence.OneToOne;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Entity
public class Invitation extends PanacheEntity {

    @Column(nullable = false, unique = true)
    public String invitationCode;

    @Column(name = "pin", nullable = false)
    public String pinHash;

    @OneToOne
    public UserAccount account;

    @Embedded
    public LoginAttempts loginAttempts = new LoginAttempts();

    @Column(nullable = false)
    public Language language;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(nullable = false, columnDefinition = "json")
    public List<Guest> guests = new ArrayList<>();

    public static Optional<Invitation> findByInvitationCode(String invitationCode) {
        return Invitation.find("invitationCode", invitationCode).firstResultOptional();
    }
}
