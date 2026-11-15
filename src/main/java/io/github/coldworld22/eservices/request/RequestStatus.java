package io.github.coldworld22.eservices.request;

import java.util.EnumSet;
import java.util.Set;

/**
 * Lifecycle of a service request. Allowed moves:
 *
 * <pre>
 * SUBMITTED --assign--> IN_REVIEW --approve--> APPROVED
 *     |                     \-----reject---> REJECTED
 *     \--cancel--> CANCELLED
 * </pre>
 */
public enum RequestStatus {
    SUBMITTED,
    IN_REVIEW,
    APPROVED,
    REJECTED,
    CANCELLED;

    public Set<RequestStatus> allowedNext() {
        return switch (this) {
            case SUBMITTED -> EnumSet.of(IN_REVIEW, CANCELLED);
            case IN_REVIEW -> EnumSet.of(APPROVED, REJECTED);
            case APPROVED, REJECTED, CANCELLED -> EnumSet.noneOf(RequestStatus.class);
        };
    }

    public boolean canMoveTo(RequestStatus next) {
        return allowedNext().contains(next);
    }

    public boolean isFinal() {
        return allowedNext().isEmpty();
    }
}
