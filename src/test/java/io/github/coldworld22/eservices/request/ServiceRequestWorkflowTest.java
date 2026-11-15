package io.github.coldworld22.eservices.request;

import static org.hamcrest.Matchers.hasItems;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import io.github.coldworld22.eservices.IntegrationTest;
import com.jayway.jsonpath.JsonPath;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class ServiceRequestWorkflowTest extends IntegrationTest {

    private String citizen;
    private String otherCitizen;
    private String employee;
    private String supervisor;
    private String admin;

    @BeforeEach
    void signIn() throws Exception {
        citizen = login(CITIZEN);
        otherCitizen = login(OTHER_CITIZEN);
        employee = login(EMPLOYEE);
        supervisor = login(SUPERVISOR);
        admin = login(ADMIN);
    }

    private String submit(String token) throws Exception {
        String response = postJson("/api/v1/requests", token, Map.of(
                        "type", "BUILDING_PERMIT",
                        "title", "Extension to family house",
                        "description", "Adding a second floor of 120 square metres."))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("SUBMITTED"))
                .andExpect(jsonPath("$.referenceNo").value(org.hamcrest.Matchers.matchesPattern("REQ-\\d{4}-\\d{6}")))
                .andReturn().getResponse().getContentAsString();
        return JsonPath.read(response, "$.id");
    }

    @Test
    void requestGoesFromSubmissionToApprovalAndIsAudited() throws Exception {
        String id = submit(citizen);

        postJson("/api/v1/requests/" + id + "/assign", employee, Map.of())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("IN_REVIEW"))
                .andExpect(jsonPath("$.assigneeId").value("00000000-0000-0000-0000-000000000002"));

        postJson("/api/v1/requests/" + id + "/decision", supervisor, Map.of("decision", "APPROVE", "note", "Plans comply."))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("APPROVED"))
                .andExpect(jsonPath("$.decisionNote").value("Plans comply."));

        mvc.perform(get("/api/v1/audit").param("entityId", id).header("Authorization", "Bearer " + admin))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalItems").value(3))
                .andExpect(jsonPath("$.items[*].action")
                        .value(hasItems("REQUEST_SUBMITTED", "REQUEST_ASSIGNED", "REQUEST_APPROVED")));
    }

    @Test
    void decisionBeforeReviewIsAConflict() throws Exception {
        String id = submit(citizen);

        postJson("/api/v1/requests/" + id + "/decision", supervisor, Map.of("decision", "APPROVE"))
                .andExpect(status().isConflict());
    }

    @Test
    void rejectionNeedsANote() throws Exception {
        String id = submit(citizen);
        postJson("/api/v1/requests/" + id + "/assign", employee, Map.of()).andExpect(status().isOk());

        postJson("/api/v1/requests/" + id + "/decision", supervisor, Map.of("decision", "REJECT"))
                .andExpect(status().isBadRequest());
        postJson("/api/v1/requests/" + id + "/decision", supervisor,
                Map.of("decision", "REJECT", "note", "Site plan is missing."))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("REJECTED"));
    }

    @Test
    void rolesAreEnforced() throws Exception {
        String id = submit(citizen);

        postJson("/api/v1/requests", employee, Map.of(
                        "type", "COMPLAINT", "title", "Staff cannot submit", "description", "Should be refused."))
                .andExpect(status().isForbidden());
        postJson("/api/v1/requests/" + id + "/assign", citizen, Map.of()).andExpect(status().isForbidden());
        postJson("/api/v1/requests/" + id + "/decision", employee, Map.of("decision", "APPROVE"))
                .andExpect(status().isForbidden());
        mvc.perform(get("/api/v1/audit").header("Authorization", "Bearer " + supervisor))
                .andExpect(status().isForbidden());
    }

    @Test
    void citizensOnlySeeTheirOwnRequests() throws Exception {
        String id = submit(citizen);

        mvc.perform(get("/api/v1/requests/" + id).header("Authorization", "Bearer " + otherCitizen))
                .andExpect(status().isNotFound());
        postJson("/api/v1/requests/" + id + "/cancel", otherCitizen, Map.of()).andExpect(status().isNotFound());
        mvc.perform(get("/api/v1/requests/" + id).header("Authorization", "Bearer " + employee))
                .andExpect(status().isOk());
    }

    @Test
    void citizenCanCancelOnlyBeforeReview() throws Exception {
        String first = submit(citizen);
        postJson("/api/v1/requests/" + first + "/cancel", citizen, Map.of())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CANCELLED"));

        String second = submit(citizen);
        postJson("/api/v1/requests/" + second + "/assign", employee, Map.of()).andExpect(status().isOk());
        postJson("/api/v1/requests/" + second + "/cancel", citizen, Map.of()).andExpect(status().isConflict());
    }

    @Test
    void invalidInputIsRejected() throws Exception {
        postJson("/api/v1/requests", citizen, Map.of("type", "BUILDING_PERMIT", "title", "", "description", "x"))
                .andExpect(status().isBadRequest());
    }
}
