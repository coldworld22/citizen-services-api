package io.github.coldworld22.eservices.request;

import io.github.coldworld22.eservices.audit.AuditAction;
import io.github.coldworld22.eservices.audit.AuditService;
import io.github.coldworld22.eservices.common.ApiException;
import io.github.coldworld22.eservices.user.Actor;
import java.time.Clock;
import java.time.Instant;
import java.time.Year;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ServiceRequestService {

    static final String ENTITY_TYPE = "SERVICE_REQUEST";

    private final ServiceRequestRepository requests;
    private final AuditService audit;
    private final Clock clock;

    public ServiceRequestService(ServiceRequestRepository requests, AuditService audit, Clock clock) {
        this.requests = requests;
        this.audit = audit;
        this.clock = clock;
    }

    @Transactional
    public ServiceRequest submit(Actor citizen, RequestType type, String title, String description) {
        Instant now = clock.instant();
        String reference = "REQ-%d-%06d".formatted(Year.now(clock).getValue(), requests.nextReferenceNumber());
        ServiceRequest request = requests.save(
                new ServiceRequest(reference, type, title.trim(), description.trim(), citizen.id(), now));
        audit.record(citizen.id(), citizen.role(), AuditAction.REQUEST_SUBMITTED, ENTITY_TYPE, request.getId(),
                "reference " + reference);
        return request;
    }

    /** Citizens see only their own requests; staff see all of them. */
    @Transactional(readOnly = true)
    public Page<ServiceRequest> list(Actor actor, RequestStatus status, Pageable pageable) {
        if (actor.isStaff()) {
            return status == null ? requests.findAll(pageable) : requests.findByStatus(status, pageable);
        }
        return status == null
                ? requests.findByCitizenId(actor.id(), pageable)
                : requests.findByCitizenIdAndStatus(actor.id(), status, pageable);
    }

    @Transactional(readOnly = true)
    public ServiceRequest get(Actor actor, UUID id) {
        return loadVisible(actor, id);
    }

    @Transactional
    public ServiceRequest assignToSelf(Actor employee, UUID id) {
        ServiceRequest request = load(id);
        request.assignTo(employee.id(), clock.instant());
        audit.record(employee.id(), employee.role(), AuditAction.REQUEST_ASSIGNED, ENTITY_TYPE, id, null);
        return request;
    }

    @Transactional
    public ServiceRequest decide(Actor supervisor, UUID id, Decision decision, String note) {
        String trimmedNote = note == null ? null : note.trim();
        if (decision == Decision.REJECT && (trimmedNote == null || trimmedNote.isEmpty())) {
            throw ApiException.badRequest("A note explaining the rejection is required.");
        }
        ServiceRequest request = load(id);
        RequestStatus outcome = decision == Decision.APPROVE ? RequestStatus.APPROVED : RequestStatus.REJECTED;
        request.decide(outcome, trimmedNote, clock.instant());
        AuditAction action = decision == Decision.APPROVE ? AuditAction.REQUEST_APPROVED : AuditAction.REQUEST_REJECTED;
        audit.record(supervisor.id(), supervisor.role(), action, ENTITY_TYPE, id, trimmedNote);
        return request;
    }

    @Transactional
    public ServiceRequest cancel(Actor citizen, UUID id) {
        ServiceRequest request = loadVisible(citizen, id);
        if (!request.isOwnedBy(citizen.id())) {
            throw new AccessDeniedException("Only the citizen who submitted a request can cancel it.");
        }
        request.cancel(clock.instant());
        audit.record(citizen.id(), citizen.role(), AuditAction.REQUEST_CANCELLED, ENTITY_TYPE, id, null);
        return request;
    }

    private ServiceRequest load(UUID id) {
        return requests.findById(id).orElseThrow(() -> ApiException.notFound("Request not found."));
    }

    /** A citizen asking for someone else's request gets 404, so request IDs cannot be probed. */
    private ServiceRequest loadVisible(Actor actor, UUID id) {
        ServiceRequest request = load(id);
        if (!actor.isStaff() && !request.isOwnedBy(actor.id())) {
            throw ApiException.notFound("Request not found.");
        }
        return request;
    }
}
