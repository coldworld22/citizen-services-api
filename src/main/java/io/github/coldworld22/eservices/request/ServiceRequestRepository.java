package io.github.coldworld22.eservices.request;

import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface ServiceRequestRepository extends JpaRepository<ServiceRequest, UUID> {

    Page<ServiceRequest> findByCitizenId(UUID citizenId, Pageable pageable);

    Page<ServiceRequest> findByCitizenIdAndStatus(UUID citizenId, RequestStatus status, Pageable pageable);

    Page<ServiceRequest> findByStatus(RequestStatus status, Pageable pageable);

    @Query(value = "select nextval('service_request_ref_seq')", nativeQuery = true)
    long nextReferenceNumber();
}
