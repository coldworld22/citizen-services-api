package io.github.coldworld22.eservices.request;

import io.github.coldworld22.eservices.common.ApiException;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "service_requests")
public class ServiceRequest {

    @Id
    private UUID id;

    @Column(name = "reference_no", nullable = false, unique = true, length = 20)
    private String referenceNo;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private RequestType type;

    @Column(nullable = false, length = 150)
    private String title;

    @Column(nullable = false, length = 2000)
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private RequestStatus status;

    @Column(name = "citizen_id", nullable = false)
    private UUID citizenId;

    @Column(name = "assignee_id")
    private UUID assigneeId;

    @Column(name = "decision_note", length = 1000)
    private String decisionNote;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    /** Optimistic locking: two staff members cannot overwrite each other's decision. */
    @Version
    private Long version;

    protected ServiceRequest() {}

    public ServiceRequest(String referenceNo, RequestType type, String title, String description,
                          UUID citizenId, Instant now) {
        this.id = UUID.randomUUID();
        this.referenceNo = referenceNo;
        this.type = type;
        this.title = title;
        this.description = description;
        this.citizenId = citizenId;
        this.status = RequestStatus.SUBMITTED;
        this.createdAt = now;
        this.updatedAt = now;
    }

    public boolean isOwnedBy(UUID userId) {
        return citizenId.equals(userId);
    }

    void assignTo(UUID employeeId, Instant now) {
        moveTo(RequestStatus.IN_REVIEW, now);
        this.assigneeId = employeeId;
    }

    void decide(RequestStatus outcome, String note, Instant now) {
        moveTo(outcome, now);
        this.decisionNote = note;
    }

    void cancel(Instant now) {
        moveTo(RequestStatus.CANCELLED, now);
    }

    private void moveTo(RequestStatus next, Instant now) {
        if (!status.canMoveTo(next)) {
            throw ApiException.conflict("A request in status %s cannot move to %s.".formatted(status, next));
        }
        this.status = next;
        this.updatedAt = now;
    }

    public UUID getId() {
        return id;
    }

    public String getReferenceNo() {
        return referenceNo;
    }

    public RequestType getType() {
        return type;
    }

    public String getTitle() {
        return title;
    }

    public String getDescription() {
        return description;
    }

    public RequestStatus getStatus() {
        return status;
    }

    public UUID getCitizenId() {
        return citizenId;
    }

    public UUID getAssigneeId() {
        return assigneeId;
    }

    public String getDecisionNote() {
        return decisionNote;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }
}
