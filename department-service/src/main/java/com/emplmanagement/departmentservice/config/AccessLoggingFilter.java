package com.emplmanagement.departmentservice.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

@Component
public class AccessLoggingFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(AccessLoggingFilter.class);
    private static final String CORRELATION_ID_HEADER = "X-Correlation-Id";

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        String correlationId = request.getHeader(CORRELATION_ID_HEADER);
        if (correlationId == null || correlationId.isBlank()) {
            correlationId = UUID.randomUUID().toString();
        }
        MDC.put("correlationId", correlationId);
        response.setHeader(CORRELATION_ID_HEADER, correlationId);

        String username = "anonymous";
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.isAuthenticated()) {
            username = auth.getName();
        }
        MDC.put("username", username);

        Instant start = Instant.now();
        String method = request.getMethod();
        String path = request.getRequestURI();
        String query = request.getQueryString();
        String clientIp = request.getRemoteAddr();

        try {
            filterChain.doFilter(request, response);
            long duration = Duration.between(start, Instant.now()).toMillis();
            int status = response.getStatus();
            log.info("ACCESS department method={} path={}{} status={} durationMs={} clientIp={}",
                    method, path, (query != null ? ("?" + query) : ""), status, duration, clientIp);
        } catch (Exception ex) {
            long duration = Duration.between(start, Instant.now()).toMillis();
            int status = response.getStatus();
            log.error("ERROR department method={} path={} status={} durationMs={} error={}",
                    method, path, status, duration, ex.toString());
            throw ex;
        } finally {
            MDC.remove("username");
            MDC.remove("correlationId");
        }
    }
}
