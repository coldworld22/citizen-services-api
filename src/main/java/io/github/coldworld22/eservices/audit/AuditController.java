package io.github.coldworld22.eservices.audit;

import io.github.coldworld22.eservices.common.PageResponse;
import io.github.coldworld22.eservices.user.Role;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.time.Instant;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/audit")
@Tag(name = "Audit trail")
public class AuditController {

    private final AuditEventRepository repository;

    public AuditController(AuditEventRepository repository) {
        this.repository = repository;
    }

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    @Transactional(readOnly = true)
    @Operation(summary = "Search the audit trail by record or by user (admins only)")
    public PageResponse<AuditEventResponse> search(
            @RequestParam(required = false) UUID entityId,
            @RequestParam(required = false) UUID actorId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size) {
        Pageable pageable = PageRequest.of(Math.max(page, 0), Math.clamp(size, 1, 200),
                Sort.by(Sort.Direction.DESC, "occurredAt"));
        Page<AuditEvent> result;
        if (entityId != null) {
            result = repository.findByEntityId(entityId, pageable);
        } else if (actorId != null) {
            result = repository.findByActorId(actorId, pageable);
        } else {
            result = repository.findAll(pageable);
        }
        return PageResponse.of(result, AuditEventResponse::from);
    }

    public record AuditEventResponse(UUID id, Instant occurredAt, UUID actorId, Role actorRole,
                                     AuditAction action, String entityType, UUID entityId,
                                     String details, String ipAddress) {

        static AuditEventResponse from(AuditEvent e) {
            return new AuditEventResponse(e.getId(), e.getOccurredAt(), e.getActorId(), e.getActorRole(),
                    e.getAction(), e.getEntityType(), e.getEntityId(), e.getDetails(), e.getIpAddress());
        }
    }
}
