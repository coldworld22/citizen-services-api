package io.github.coldworld22.eservices.request;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class RequestStatusTest {

    @Test
    void submittedCanOnlyGoToReviewOrBeCancelled() {
        assertThat(RequestStatus.SUBMITTED.allowedNext())
                .containsExactlyInAnyOrder(RequestStatus.IN_REVIEW, RequestStatus.CANCELLED);
    }

    @Test
    void inReviewCanOnlyBeApprovedOrRejected() {
        assertThat(RequestStatus.IN_REVIEW.allowedNext())
                .containsExactlyInAnyOrder(RequestStatus.APPROVED, RequestStatus.REJECTED);
    }

    @Test
    void decidedRequestsAreFinal() {
        assertThat(RequestStatus.APPROVED.isFinal()).isTrue();
        assertThat(RequestStatus.REJECTED.isFinal()).isTrue();
        assertThat(RequestStatus.CANCELLED.isFinal()).isTrue();
        assertThat(RequestStatus.SUBMITTED.canMoveTo(RequestStatus.APPROVED)).isFalse();
    }
}
