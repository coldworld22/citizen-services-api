package io.github.coldworld22.eservices.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.Instant;
import java.util.UUID;

record CreateRequestBody(
        @NotNull RequestType type,
        @NotBlank @Size(max = 150) String title,
        @NotBlank @Size(max = 2000) String description) {}

enum Decision { APPROVE, REJECT }

record DecisionBody(@NotNull Decision decision, @Size(max = 1000) String note) {}

record ServiceRequestResponse(
        UUID id,
        String referenceNo,
        RequestType type,
        String title,
        String description,
        RequestStatus status,
        UUID citizenId,
        UUID assigneeId,
        String decisionNote,
        Instant createdAt,
        Instant updatedAt) {

    static ServiceRequestResponse from(ServiceRequest r) {
        return new ServiceRequestResponse(r.getId(), r.getReferenceNo(), r.getType(), r.getTitle(),
                r.getDescription(), r.getStatus(), r.getCitizenId(), r.getAssigneeId(), r.getDecisionNote(),
                r.getCreatedAt(), r.getUpdatedAt());
    }
}
