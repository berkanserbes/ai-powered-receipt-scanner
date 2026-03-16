package com.berkan.receiptscanner.filter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import tools.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

@Component
public class RequestRateLimitFilter extends OncePerRequestFilter {

    private final ObjectMapper objectMapper;
    private final int maxRequests;
    private final long windowMillis;

    private final ConcurrentHashMap<String, ClientRequestWindow> clientWindows = new ConcurrentHashMap<>();
    private final AtomicInteger cleanupCounter = new AtomicInteger(0);

    public RequestRateLimitFilter(
            ObjectMapper objectMapper,
            @Value("${security.rate-limit.max-requests:120}") int maxRequests,
            @Value("${security.rate-limit.window-seconds:60}") int windowSeconds) {
        this.objectMapper = objectMapper;
        this.maxRequests = Math.max(1, maxRequests);
        this.windowMillis = Math.max(1, windowSeconds) * 1000L;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain) throws ServletException, IOException {
        if ("OPTIONS".equalsIgnoreCase(request.getMethod()) || isExcludedPath(request.getRequestURI())) {
            filterChain.doFilter(request, response);
            return;
        }

        long now = System.currentTimeMillis();
        String clientKey = resolveClientKey(request);

        ClientRequestWindow window = clientWindows.computeIfAbsent(clientKey, key -> new ClientRequestWindow(now));
        boolean allowed = window.tryConsume(now, windowMillis, maxRequests);

        response.setHeader("X-RateLimit-Limit", String.valueOf(maxRequests));
        response.setHeader("X-RateLimit-Window-Seconds", String.valueOf(windowMillis / 1000L));

        if (!allowed) {
            response.setStatus(429);
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            response.getWriter().write(objectMapper.writeValueAsString(Map.of(
                    "success", false,
                    "message", "Too many requests",
                    "error", Map.of(
                            "code", "RATE_LIMIT_EXCEEDED",
                            "details", "Rate limit exceeded. Please try again later."),
                    "timestamp", Instant.now().toString())));
            return;
        }

        maybeCleanup(now);
        filterChain.doFilter(request, response);
    }

    private void maybeCleanup(long now) {
        int current = cleanupCounter.incrementAndGet();
        if (current % 500 != 0) {
            return;
        }
        long staleThreshold = now - (windowMillis * 2);
        clientWindows.entrySet().removeIf(entry -> entry.getValue().getWindowStart() < staleThreshold);
    }

    private static boolean isExcludedPath(String path) {
        return path.startsWith("/swagger-ui")
                || path.startsWith("/api-docs")
                || path.equals("/swagger-ui.html");
    }

    private static String resolveClientKey(HttpServletRequest request) {
        String forwardedFor = request.getHeader("X-Forwarded-For");
        if (forwardedFor != null && !forwardedFor.isBlank()) {
            return forwardedFor.split(",")[0].trim();
        }
        String realIp = request.getHeader("X-Real-IP");
        if (realIp != null && !realIp.isBlank()) {
            return realIp.trim();
        }
        return request.getRemoteAddr();
    }

    private static class ClientRequestWindow {
        private long windowStart;
        private int requestCount;

        private ClientRequestWindow(long windowStart) {
            this.windowStart = windowStart;
            this.requestCount = 0;
        }

        private synchronized boolean tryConsume(long now, long windowMillis, int maxRequests) {
            if (now - windowStart >= windowMillis) {
                windowStart = now;
                requestCount = 0;
            }
            requestCount++;
            return requestCount <= maxRequests;
        }

        private synchronized long getWindowStart() {
            return windowStart;
        }
    }
}
