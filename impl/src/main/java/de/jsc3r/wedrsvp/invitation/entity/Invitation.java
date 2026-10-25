package de.jsc3r.wedrsvp.invitation.entity;

import io.quarkus.hibernate.orm.panache.PanacheEntity;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Entity
public class Invitation extends PanacheEntity {

    @Column(nullable = false, unique = true)
    public String invitationCode;

    @Column(nullable = false)
    public String pin;

    @Column(nullable = false)
    public Language language;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(nullable = false, columnDefinition = "json")
    public List<Guest> guests = new ArrayList<>();

    public static Optional<Invitation> findByInvitationCode(String invitationCode) {
        return Invitation.find("invitationCode", invitationCode).firstResultOptional();
    }
}
