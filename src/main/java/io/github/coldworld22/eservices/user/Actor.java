package io.github.coldworld22.eservices.user;

import java.util.UUID;
import org.springframework.security.oauth2.jwt.Jwt;

/** The signed-in user making a request, taken from the access token. */
public record Actor(UUID id, Role role) {

    public static Actor from(Jwt jwt) {
        return new Actor(UUID.fromString(jwt.getSubject()), Role.valueOf(jwt.getClaimAsString("role")));
    }

    public boolean isStaff() {
        return role.isStaff();
    }
}
