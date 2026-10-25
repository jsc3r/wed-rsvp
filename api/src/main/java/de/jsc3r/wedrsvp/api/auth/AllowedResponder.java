package de.jsc3r.wedrsvp.api.auth;

import jakarta.ws.rs.NameBinding;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Allows administrators to access any invitation and guests to access their own invitation.
 * Adds invitation ownership checks to endpoints already protected with {@code @RolesAllowed}.
 * Endpoints must expose an {@code invitation-code} path parameter.
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE, ElementType.METHOD})
@NameBinding
public @interface AllowedResponder {
}
