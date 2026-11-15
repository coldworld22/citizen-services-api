package io.github.coldworld22.eservices.auth;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import io.github.coldworld22.eservices.IntegrationTest;
import com.jayway.jsonpath.JsonPath;
import java.util.Map;
import org.junit.jupiter.api.Test;

/** Staff sign-in against an in-memory LDAP directory standing in for Active Directory. */
class StaffDirectoryLoginTest extends IntegrationTest {

    private static final String URL = "/api/v1/auth/staff/login";

    @Test
    void staffSignInWithDirectoryPasswordAndGetTheirAppRole() throws Exception {
        String response = postJson(URL, Map.of("username", "employee1", "password", "Employee#2026"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.role").value("EMPLOYEE"))
                .andReturn().getResponse().getContentAsString();
        String token = JsonPath.read(response, "$.accessToken");

        mvc.perform(get("/api/v1/requests").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());
    }

    @Test
    void usernameIsCaseInsensitiveLikeActiveDirectory() throws Exception {
        postJson(URL, Map.of("username", "Supervisor1", "password", "Supervisor#2026"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.role").value("SUPERVISOR"));
    }

    @Test
    void wrongPasswordIsRejected() throws Exception {
        postJson(URL, Map.of("username", "employee1", "password", "not-the-password"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void blankPasswordIsRejectedInsteadOfBecomingAnAnonymousBind() throws Exception {
        postJson(URL, Map.of("username", "employee1", "password", " "))
                .andExpect(status().isBadRequest());
    }

    @Test
    void directoryAccountWithoutAnAppAccountGetsNothing() throws Exception {
        postJson(URL, Map.of("username", "contractor1", "password", "Contractor#2026"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void ldapInjectionInTheUsernameDoesNotMatchAnyone() throws Exception {
        postJson(URL, Map.of("username", "*)(uid=*", "password", "Employee#2026"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void unknownUserIsRejected() throws Exception {
        postJson(URL, Map.of("username", "nobody", "password", "whatever"))
                .andExpect(status().isUnauthorized());
    }
}
