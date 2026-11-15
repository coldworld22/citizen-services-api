package io.github.coldworld22.eservices.auth;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

/** A one-time code sent to a user. Only a hash of the code is stored. */
@Entity
@Table(name = "otp_challenges")
public class OtpChallenge {

    @Id
    private UUID id;

    @Column(name = "national_id", nullable = false, length = 10)
    private String nationalId;

    @Column(name = "code_hash", nullable = false, length = 100)
    private String codeHash;

    @Column(nullable = false)
    private int attempts;

    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;

    @Column(name = "consumed_at")
    private Instant consumedAt;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    protected OtpChallenge() {}

    public OtpChallenge(String nationalId, String codeHash, Instant createdAt, Instant expiresAt) {
        this.id = UUID.randomUUID();
        this.nationalId = nationalId;
        this.codeHash = codeHash;
        this.createdAt = createdAt;
        this.expiresAt = expiresAt;
    }

    public boolean isExpired(Instant now) {
        return !expiresAt.isAfter(now);
    }

    public void registerAttempt() {
        attempts++;
    }

    public void consume(Instant now) {
        consumedAt = now;
    }

    public UUID getId() {
        return id;
    }

    public String getNationalId() {
        return nationalId;
    }

    public String getCodeHash() {
        return codeHash;
    }

    public int getAttempts() {
        return attempts;
    }

    public Instant getExpiresAt() {
        return expiresAt;
    }
}
