package io.github.coldworld22.eservices.config;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import io.github.coldworld22.eservices.IntegrationTest;
import org.junit.jupiter.api.Test;

class SecurityHeadersTest extends IntegrationTest {

    @Test
    void responsesCarryHardeningHeaders() throws Exception {
        mvc.perform(get("/actuator/health"))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Security-Policy", SecurityConfig.CONTENT_SECURITY_POLICY))
                .andExpect(header().string("X-Content-Type-Options", "nosniff"))
                .andExpect(header().string("X-Frame-Options", "DENY"))
                .andExpect(header().string("Referrer-Policy", "no-referrer"))
                .andExpect(header().string("Cache-Control", org.hamcrest.Matchers.containsString("no-store")));
    }

    @Test
    void errorsDoNotLeakStackTraces() throws Exception {
        mvc.perform(get("/api/v1/requests/not-a-uuid").header("Authorization", "Bearer " + login(CITIZEN)))
                .andExpect(status().isBadRequest())
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers
                        .jsonPath("$.trace").doesNotExist());
    }
}
