package de.jsc3r.wedrsvp.auth.entity;

import de.jsc3r.wedrsvp.invitation.entity.Invitation;
import io.quarkus.hibernate.orm.panache.PanacheEntity;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.OneToOne;

import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

@Entity
public class UserAccount extends PanacheEntity {

    @Column(unique = true)
    public String username;

    public String passwordHash;

    @ElementCollection(fetch = FetchType.EAGER)
    @Column(nullable = false)
    public Set<String> roles = new HashSet<>();

    @Embedded
    public LoginAttempts loginAttempts = new LoginAttempts();

    @OneToOne(mappedBy = "account")
    public Invitation invitation;

    public static Optional<UserAccount> findByUsername(String username) {
        return find("username", username).firstResultOptional();
    }
}
