package de.jsc3r.wedrsvp.auth.boundary;

import jakarta.annotation.security.RolesAllowed;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;

@Path("api/test-security")
public class SecurityTestResource {

    @GET
    @Path("admin")
    @RolesAllowed("admin")
    public String admin() {
        return "admin";
    }

    @GET
    @Path("unannotated")
    public String unannotated() {
        return "unannotated";
    }
}
