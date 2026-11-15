package io.github.coldworld22.eservices.auth;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OtpChallengeRepository extends JpaRepository<OtpChallenge, UUID> {

    Optional<OtpChallenge> findFirstByNationalIdAndConsumedAtIsNullOrderByCreatedAtDesc(String nationalId);

    long countByNationalIdAndCreatedAtAfter(String nationalId, Instant since);
}
