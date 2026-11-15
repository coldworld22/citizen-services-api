package io.github.coldworld22.eservices.config;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

class AuthRateLimitFilterTest {

    private static final Clock CLOCK = Clock.fixed(Instant.parse("2026-11-15T10:00:10Z"), ZoneOffset.UTC);

    private final AuthRateLimitFilter filter =
            new AuthRateLimitFilter(new AppProperties(null, null, null, new AppProperties.RateLimit(3)), CLOCK);

    private MockHttpServletResponse call(String path, String ip) throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", path);
        request.setRemoteAddr(ip);
        MockHttpServletResponse response = new MockHttpServletResponse();
        filter.doFilter(request, response, new MockFilterChain());
        return response;
    }

    @Test
    void blocksAnAddressAfterTheLimitWithRetryAfter() throws Exception {
        for (int i = 0; i < 3; i++) {
            assertThat(call("/api/v1/auth/otp/request", "10.0.0.1").getStatus()).isEqualTo(200);
        }
        MockHttpServletResponse blocked = call("/api/v1/auth/otp/request", "10.0.0.1");

        assertThat(blocked.getStatus()).isEqualTo(429);
        assertThat(blocked.getHeader("Retry-After")).isEqualTo("50");
        assertThat(blocked.getContentType()).isEqualTo("application/problem+json");
    }

    @Test
    void countsEachAddressSeparately() throws Exception {
        for (int i = 0; i < 3; i++) {
            call("/api/v1/auth/staff/login", "10.0.0.2");
        }
        assertThat(call("/api/v1/auth/staff/login", "10.0.0.3").getStatus()).isEqualTo(200);
    }

    @Test
    void leavesOtherEndpointsAlone() throws Exception {
        for (int i = 0; i < 10; i++) {
            assertThat(call("/api/v1/requests", "10.0.0.4").getStatus()).isEqualTo(200);
        }
    }
}
