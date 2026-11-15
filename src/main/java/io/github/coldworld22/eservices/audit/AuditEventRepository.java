package io.github.coldworld22.eservices.audit;

import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AuditEventRepository extends JpaRepository<AuditEvent, UUID> {

    Page<AuditEvent> findByEntityId(UUID entityId, Pageable pageable);

    Page<AuditEvent> findByActorId(UUID actorId, Pageable pageable);
}
