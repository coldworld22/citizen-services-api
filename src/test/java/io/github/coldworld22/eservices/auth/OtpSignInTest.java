package io.github.coldworld22.eservices.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import io.github.coldworld22.eservices.IntegrationTest;
import java.util.Map;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;

class OtpSignInTest extends IntegrationTest {

    @Autowired
    private OtpChallengeRepository challenges;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Test
    void validCodeReturnsTokenWithRole() throws Exception {
        String code = requestCode(CITIZEN);

        postJson("/api/v1/auth/otp/verify", Map.of("nationalId", CITIZEN, "code", code))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.tokenType").value("Bearer"))
                .andExpect(jsonPath("$.role").value("CITIZEN"))
                .andExpect(jsonPath("$.accessToken").isNotEmpty());
    }

    @Test
    void codeCannotBeUsedTwice() throws Exception {
        String code = requestCode(CITIZEN);
        Map<String, String> body = Map.of("nationalId", CITIZEN, "code", code);

        postJson("/api/v1/auth/otp/verify", body).andExpect(status().isOk());
        postJson("/api/v1/auth/otp/verify", body).andExpect(status().isUnauthorized());
    }

    @Test
    void wrongCodesAreLimited() throws Exception {
        String code = requestCode(EMPLOYEE);
        String wrong = code.equals("000000") ? "111111" : "000000";
        Map<String, String> body = Map.of("nationalId", EMPLOYEE, "code", wrong);

        for (int i = 0; i < 5; i++) {
            postJson("/api/v1/auth/otp/verify", body).andExpect(status().isUnauthorized());
        }
        // Even the right code is refused once the attempts are used up.
        postJson("/api/v1/auth/otp/verify", Map.of("nationalId", EMPLOYEE, "code", code))
                .andExpect(status().isTooManyRequests());
    }

    @Test
    void unknownIdGetsTheSameAnswerWithoutACode() throws Exception {
        postJson("/api/v1/auth/otp/request", Map.of("nationalId", "1999999999"))
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.message").value(OtpService.GENERIC_MESSAGE))
                .andExpect(jsonPath("$.devCode").value(Matchers.nullValue()));
    }

    @Test
    void malformedIdIsRejected() throws Exception {
        postJson("/api/v1/auth/otp/request", Map.of("nationalId", "12345"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void protectedEndpointsNeedAToken() throws Exception {
        mvc.perform(get("/api/v1/requests")).andExpect(status().isUnauthorized());
    }

    @Test
    void codesAreStoredOnlyAsHashes() throws Exception {
        String code = requestCode(SUPERVISOR);

        OtpChallenge stored = challenges
                .findFirstByNationalIdAndConsumedAtIsNullOrderByCreatedAtDesc(SUPERVISOR)
                .orElseThrow();
        assertThat(stored.getCodeHash()).isNotEqualTo(code).doesNotContain(code);
        assertThat(passwordEncoder.matches(code, stored.getCodeHash())).isTrue();
    }
}
