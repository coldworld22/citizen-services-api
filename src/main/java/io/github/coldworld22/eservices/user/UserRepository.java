package io.github.coldworld22.eservices.user;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserRepository extends JpaRepository<User, UUID> {

    Optional<User> findByNationalIdAndActiveTrue(String nationalId);

    Optional<User> findByDirectoryUsernameIgnoreCaseAndActiveTrue(String directoryUsername);
}
