package io.github.coldworld22.eservices.request;

import io.github.coldworld22.eservices.common.PageResponse;
import io.github.coldworld22.eservices.user.Actor;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.UUID;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/requests")
@Tag(name = "Service requests")
public class ServiceRequestController {

    private final ServiceRequestService service;

    public ServiceRequestController(ServiceRequestService service) {
        this.service = service;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('CITIZEN')")
    @Operation(summary = "Submit a new request (citizens)")
    public ServiceRequestResponse submit(@AuthenticationPrincipal Jwt jwt, @Valid @RequestBody CreateRequestBody body) {
        return ServiceRequestResponse.from(
                service.submit(Actor.from(jwt), body.type(), body.title(), body.description()));
    }

    @GetMapping
    @Operation(summary = "List requests: your own as a citizen, all of them as staff")
    public PageResponse<ServiceRequestResponse> list(
            @AuthenticationPrincipal Jwt jwt,
            @RequestParam(required = false) RequestStatus status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        PageRequest pageable = PageRequest.of(Math.max(page, 0), Math.clamp(size, 1, 100),
                Sort.by(Sort.Direction.DESC, "createdAt"));
        return PageResponse.of(service.list(Actor.from(jwt), status, pageable), ServiceRequestResponse::from);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get one request")
    public ServiceRequestResponse get(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID id) {
        return ServiceRequestResponse.from(service.get(Actor.from(jwt), id));
    }

    @PostMapping("/{id}/assign")
    @PreAuthorize("hasAnyRole('EMPLOYEE', 'SUPERVISOR')")
    @Operation(summary = "Take a submitted request for review (employees)")
    public ServiceRequestResponse assign(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID id) {
        return ServiceRequestResponse.from(service.assignToSelf(Actor.from(jwt), id));
    }

    @PostMapping("/{id}/decision")
    @PreAuthorize("hasRole('SUPERVISOR')")
    @Operation(summary = "Approve or reject a request under review (supervisors)")
    public ServiceRequestResponse decide(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID id,
                                         @Valid @RequestBody DecisionBody body) {
        return ServiceRequestResponse.from(service.decide(Actor.from(jwt), id, body.decision(), body.note()));
    }

    @PostMapping("/{id}/cancel")
    @PreAuthorize("hasRole('CITIZEN')")
    @Operation(summary = "Cancel your own request before review starts (citizens)")
    public ServiceRequestResponse cancel(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID id) {
        return ServiceRequestResponse.from(service.cancel(Actor.from(jwt), id));
    }
}
