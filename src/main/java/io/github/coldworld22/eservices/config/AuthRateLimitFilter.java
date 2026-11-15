package io.github.coldworld22.eservices.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.time.Clock;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Fixed-window rate limit on the sign-in endpoints, per client IP. It slows down password and code
 * guessing on top of the per-code attempt limit.
 *
 * <p>Counters live in this instance's memory, so with several instances each enforces its own limit.
 * In production, put a shared limit in front as well (for example an AWS WAF rate-based rule).
 */
@Component
public class AuthRateLimitFilter extends OncePerRequestFilter {

    private static final String AUTH_PREFIX = "/api/v1/auth/";
    private static final int MAX_TRACKED_CLIENTS = 10_000;

    private final int limitPerMinute;
    private final Clock clock;
    private final Map<String, Window> windows = new ConcurrentHashMap<>();

    public AuthRateLimitFilter(AppProperties properties, Clock clock) {
        this.limitPerMinute = properties.rateLimit().authRequestsPerMinute();
        this.clock = clock;
    }

    private record Window(long minute, AtomicInteger count) {}

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return !request.getRequestURI().startsWith(AUTH_PREFIX);
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        long nowMillis = clock.millis();
        long minute = nowMillis / 60_000;
        if (windows.size() > MAX_TRACKED_CLIENTS) {
            windows.values().removeIf(w -> w.minute() < minute);
        }

        Window window = windows.compute(request.getRemoteAddr(),
                (ip, current) -> current == null || current.minute() != minute
                        ? new Window(minute, new AtomicInteger())
                        : current);

        if (window.count().incrementAndGet() > limitPerMinute) {
            long retryAfter = 60 - (nowMillis / 1000) % 60;
            response.setStatus(429);
            response.setHeader("Retry-After", String.valueOf(retryAfter));
            response.setContentType("application/problem+json");
            response.getWriter().write("{\"type\":\"about:blank\",\"title\":\"Too Many Requests\",\"status\":429,"
                    + "\"detail\":\"Too many sign-in attempts from this address. Try again shortly.\"}");
            return;
        }
        chain.doFilter(request, response);
    }
}
