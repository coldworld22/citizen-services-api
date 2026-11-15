package io.github.coldworld22.eservices.audit;

import io.github.coldworld22.eservices.user.Role;
import jakarta.servlet.http.HttpServletRequest;
import java.time.Clock;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

@Service
public class AuditService {

    private final AuditEventRepository repository;
    private final Clock clock;

    public AuditService(AuditEventRepository repository, Clock clock) {
        this.repository = repository;
        this.clock = clock;
    }

    /**
     * Records an event in the caller's transaction, so an audit entry is never written for a change
     * that was rolled back.
     */
    @Transactional(propagation = Propagation.MANDATORY)
    public void record(UUID actorId, Role actorRole, AuditAction action,
                       String entityType, UUID entityId, String details) {
        repository.save(new AuditEvent(
                clock.instant(), actorId, actorRole, action, entityType, entityId, details, currentIp()));
    }

    private static String currentIp() {
        if (RequestContextHolder.getRequestAttributes() instanceof ServletRequestAttributes attributes) {
            HttpServletRequest request = attributes.getRequest();
            return request.getRemoteAddr();
        }
        return null;
    }
}
