package de.jsc3r.wedrsvp.invitation.boundary;

import jakarta.validation.ConstraintViolationException;
import jakarta.ws.rs.core.Response;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ValidationExceptionHandlerTest {

    @Test
    void mapsConstraintViolationsToBadRequest() {
        ConstraintViolationException exception = new ConstraintViolationException(java.util.Set.of());

        try (Response response = new ValidationExceptionHandler().toResponse(exception)) {
            assertEquals(Response.Status.BAD_REQUEST.getStatusCode(), response.getStatus());
        }
    }
}
