package io.github.coldworld22.eservices.auth;

import io.github.coldworld22.eservices.audit.AuditAction;
import io.github.coldworld22.eservices.audit.AuditService;
import io.github.coldworld22.eservices.common.ApiException;
import io.github.coldworld22.eservices.config.AppProperties;
import io.github.coldworld22.eservices.user.User;
import io.github.coldworld22.eservices.user.UserRepository;
import java.security.SecureRandom;
import java.time.Clock;
import java.time.Instant;
import java.util.Optional;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Passwordless sign-in: the user enters their national ID, receives a 6-digit code, and exchanges it
 * for an access token. Codes are hashed at rest, expire, allow a limited number of guesses, and the
 * number of codes per ID is rate limited.
 */
@Service
public class OtpService {

    static final String GENERIC_MESSAGE = "If this ID is registered, a verification code has been sent.";
    private static final String INVALID_CODE = "The code is invalid or has expired.";

    private final UserRepository users;
    private final OtpChallengeRepository challenges;
    private final OtpSender sender;
    private final PasswordEncoder encoder;
    private final TokenService tokens;
    private final AuditService audit;
    private final AppProperties.Otp settings;
    private final Clock clock;
    private final SecureRandom random = new SecureRandom();

    public OtpService(UserRepository users, OtpChallengeRepository challenges, OtpSender sender,
                      PasswordEncoder encoder, TokenService tokens, AuditService audit,
                      AppProperties properties, Clock clock) {
        this.users = users;
        this.challenges = challenges;
        this.sender = sender;
        this.encoder = encoder;
        this.tokens = tokens;
        this.audit = audit;
        this.settings = properties.otp();
        this.clock = clock;
    }

    @Transactional
    public OtpRequestResponse requestCode(String nationalId) {
        Instant now = clock.instant();
        long expiresIn = settings.ttl().toSeconds();

        Optional<User> found = users.findByNationalIdAndActiveTrue(nationalId);
        if (found.isEmpty()) {
            // Same answer as for a real user, so the endpoint cannot be used to discover registered IDs.
            return new OtpRequestResponse(GENERIC_MESSAGE, expiresIn, null);
        }
        User user = found.get();

        long recent = challenges.countByNationalIdAndCreatedAtAfter(nationalId, now.minus(settings.window()));
        if (recent >= settings.maxRequestsPerWindow()) {
            throw ApiException.tooManyRequests("Too many codes requested. Please try again later.");
        }

        String code = "%06d".formatted(random.nextInt(1_000_000));
        challenges.save(new OtpChallenge(nationalId, encoder.encode(code), now, now.plus(settings.ttl())));
        sender.send(user, code);
        audit.record(user.getId(), user.getRole(), AuditAction.OTP_REQUESTED, "USER", user.getId(), null);

        return new OtpRequestResponse(GENERIC_MESSAGE, expiresIn, settings.exposeCode() ? code : null);
    }

    /** Failed attempts are committed even though the method throws, so the attempt limit holds. */
    @Transactional(noRollbackFor = ApiException.class)
    public TokenResponse verifyCode(String nationalId, String code) {
        Instant now = clock.instant();
        OtpChallenge challenge = challenges
                .findFirstByNationalIdAndConsumedAtIsNullOrderByCreatedAtDesc(nationalId)
                .filter(c -> !c.isExpired(now))
                .orElseThrow(() -> ApiException.unauthorized(INVALID_CODE));

        if (challenge.getAttempts() >= settings.maxAttempts()) {
            throw ApiException.tooManyRequests("Too many wrong attempts. Please request a new code.");
        }
        challenge.registerAttempt();

        User user = users.findByNationalIdAndActiveTrue(nationalId)
                .orElseThrow(() -> ApiException.unauthorized(INVALID_CODE));

        if (!encoder.matches(code, challenge.getCodeHash())) {
            audit.record(user.getId(), user.getRole(), AuditAction.LOGIN_FAILED, "USER", user.getId(),
                    "attempt " + challenge.getAttempts() + " of " + settings.maxAttempts());
            throw ApiException.unauthorized(INVALID_CODE);
        }

        challenge.consume(now);
        audit.record(user.getId(), user.getRole(), AuditAction.LOGIN_SUCCEEDED, "USER", user.getId(), null);
        return tokens.issue(user);
    }
}
