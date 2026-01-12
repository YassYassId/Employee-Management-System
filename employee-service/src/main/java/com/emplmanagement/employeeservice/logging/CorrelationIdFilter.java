package com.emplmanagement.employeeservice.logging;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.core.annotation.Order;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Optional;
import java.util.UUID;

@Component
@Order(1)
public class CorrelationIdFilter extends OncePerRequestFilter {

    public static final String CORRELATION_ID_HEADER = "X-Correlation-Id";
    private static final Logger ACCESS_LOG = LoggerFactory.getLogger("ACCESS_LOG");

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        long start = System.currentTimeMillis();

        String corrId = Optional.ofNullable(request.getHeader(CORRELATION_ID_HEADER))
                .orElse(UUID.randomUUID().toString());
        response.setHeader(CORRELATION_ID_HEADER, corrId);

        String user = extractUsername().orElse("anonymous");
        MDC.put("correlationId", corrId);
        MDC.put("user", user);
        try {
            filterChain.doFilter(request, response);
        } finally {
            long duration = System.currentTimeMillis() - start;
            int status = response.getStatus();
            String method = request.getMethod();
            String path = request.getRequestURI();
            String ip = Optional.ofNullable(request.getHeader("X-Forwarded-For")).orElse(request.getRemoteAddr());
            String ua = Optional.ofNullable(request.getHeader("User-Agent")).orElse("");

            if (status >= 500) {
                ACCESS_LOG.error("method={} path={} status={} duration_ms={} user={} ip={} ua=\"{}\" corrId={}",
                        method, path, status, duration, user, ip, ua, corrId);
            } else {
                ACCESS_LOG.info("method={} path={} status={} duration_ms={} user={} ip={} ua=\"{}\" corrId={}",
                        method, path, status, duration, user, ip, ua, corrId);
            }
            MDC.remove("correlationId");
            MDC.remove("user");
        }
    }

    private Optional<String> extractUsername() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth instanceof JwtAuthenticationToken jwt) {
            Object preferred = jwt.getToken().getClaim("preferred_username");
            if (preferred == null) preferred = jwt.getName();
            return Optional.ofNullable(preferred).map(Object::toString);
        }
        return auth != null ? Optional.ofNullable(auth.getName()) : Optional.empty();
    }
}
