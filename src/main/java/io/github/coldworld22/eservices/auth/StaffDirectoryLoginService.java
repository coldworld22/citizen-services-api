package io.github.coldworld22.eservices.auth;

import io.github.coldworld22.eservices.audit.AuditAction;
import io.github.coldworld22.eservices.audit.AuditService;
import io.github.coldworld22.eservices.common.ApiException;
import io.github.coldworld22.eservices.config.AppProperties;
import io.github.coldworld22.eservices.user.User;
import io.github.coldworld22.eservices.user.UserRepository;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.ldap.core.LdapTemplate;
import org.springframework.ldap.filter.EqualsFilter;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Staff sign in with their Active Directory (LDAP) username and password, the way employees do in
 * most government organisations. The directory proves who they are; the application's own user
 * table decides what they may do, so a directory account alone grants nothing.
 */
@Service
public class StaffDirectoryLoginService {

    private static final Logger log = LoggerFactory.getLogger(StaffDirectoryLoginService.class);
    private static final String INVALID = "Invalid username or password.";

    private final LdapTemplate ldap;
    private final UserRepository users;
    private final TokenService tokens;
    private final AuditService audit;
    private final AppProperties.Directory directory;

    public StaffDirectoryLoginService(LdapTemplate ldap, UserRepository users, TokenService tokens,
                                      AuditService audit, AppProperties properties) {
        this.ldap = ldap;
        this.users = users;
        this.tokens = tokens;
        this.audit = audit;
        this.directory = properties.directory();
    }

    @Transactional(noRollbackFor = ApiException.class)
    public TokenResponse login(String username, String password) {
        // An LDAP bind with an empty password is an "unauthenticated bind" that many servers accept.
        if (password == null || password.isBlank()) {
            throw ApiException.unauthorized(INVALID);
        }

        Optional<User> account = users.findByDirectoryUsernameIgnoreCaseAndActiveTrue(username)
                .filter(user -> user.getRole().isStaff());

        if (account.isEmpty() || !passwordAccepted(username, password)) {
            account.ifPresent(user -> audit.record(user.getId(), user.getRole(), AuditAction.LOGIN_FAILED,
                    "USER", user.getId(), "method=directory"));
            throw ApiException.unauthorized(INVALID);
        }

        User user = account.get();
        audit.record(user.getId(), user.getRole(), AuditAction.LOGIN_SUCCEEDED, "USER", user.getId(),
                "method=directory");
        return tokens.issue(user);
    }

    private boolean passwordAccepted(String username, String password) {
        // EqualsFilter escapes the value, so a crafted username cannot change the LDAP query.
        String filter = new EqualsFilter(directory.usernameAttribute(), username).encode();
        try {
            return ldap.authenticate(directory.userBase(), filter, password);
        } catch (RuntimeException e) {
            log.warn("Directory unavailable during staff sign-in", e);
            throw new ApiException(HttpStatus.SERVICE_UNAVAILABLE,
                    "The staff directory is not reachable. Please try again later.");
        }
    }
}
