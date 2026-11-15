package io.github.coldworld22.eservices.config;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app")
public record AppProperties(Jwt jwt, Otp otp, Directory directory, RateLimit rateLimit) {

    /**
     * @param secret base64-encoded HMAC key, at least 32 bytes
     * @param ttl    lifetime of an access token
     */
    public record Jwt(String secret, Duration ttl) {}

    /**
     * @param ttl                  how long a one-time code stays valid
     * @param maxAttempts          wrong guesses allowed per code
     * @param maxRequestsPerWindow codes a national ID may request per window
     * @param window               rate-limit window for code requests
     * @param exposeCode           demo only: return the code in the API response instead of only sending it
     */
    public record Otp(Duration ttl, int maxAttempts, int maxRequestsPerWindow, Duration window, boolean exposeCode) {}

    /**
     * Staff directory (Active Directory or any LDAP server). Connection settings live under
     * spring.ldap.*; these say where staff accounts are and which attribute holds the login name.
     *
     * @param userBase          where staff accounts live, relative to spring.ldap.base, e.g. ou=staff
     * @param usernameAttribute sAMAccountName for Active Directory, uid for most other LDAP servers
     */
    public record Directory(String userBase, String usernameAttribute) {}

    /** @param authRequestsPerMinute sign-in requests allowed per client IP per minute */
    public record RateLimit(int authRequestsPerMinute) {}
}
