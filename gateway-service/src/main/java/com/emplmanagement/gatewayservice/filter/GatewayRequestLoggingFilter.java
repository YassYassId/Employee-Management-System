package com.emplmanagement.gatewayservice.filter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.HttpHeaders;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.ReactiveSecurityContextHolder;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.util.UUID;


@Component
public class GatewayRequestLoggingFilter implements GlobalFilter, Ordered {

    private static final Logger ACCESS_LOG = LoggerFactory.getLogger("ACCESS_LOG");
    private static final Logger logger = LoggerFactory.getLogger(GatewayRequestLoggingFilter.class);

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        ServerHttpRequest request = exchange.getRequest();
        ServerHttpResponse response = exchange.getResponse();

        long startTime = System.currentTimeMillis();

        // Generate or extract correlation ID
        String correlationId = getCorrelationId(request);

        // Add correlation ID to request for downstream services
        ServerHttpRequest mutatedRequest = request.mutate()
                .header("X-Correlation-ID", correlationId)
                .build();

        // Add correlation ID to response
        response.getHeaders().add("X-Correlation-ID", correlationId);

        // Extract username from JWT and log request
        return ReactiveSecurityContextHolder.getContext()
                .map(SecurityContext::getAuthentication)
                .defaultIfEmpty(createAnonymousAuth())
                .flatMap(authentication -> {
                    String username = extractUsername(authentication);

                    // Log incoming request
                    logRequest(request, correlationId, username);

                    // Continue with the request and log response
                    return chain.filter(exchange.mutate().request(mutatedRequest).build())
                            .doOnSuccess(aVoid -> {
                                long duration = System.currentTimeMillis() - startTime;
                                logResponse(request, response, duration, correlationId, username);
                            })
                            .doOnError(error -> {
                                long duration = System.currentTimeMillis() - startTime;
                                logError(request, error, duration, correlationId, username);
                            });
                });
    }

    private String getCorrelationId(ServerHttpRequest request) {
        HttpHeaders headers = request.getHeaders();
        String correlationId = headers.getFirst("X-Correlation-ID");

        if (correlationId == null || correlationId.isEmpty()) {
            correlationId = UUID.randomUUID().toString();
        }

        return correlationId;
    }

    private void logRequest(ServerHttpRequest request, String correlationId, String username) {
        String method = request.getMethod().name();
        String path = request.getURI().getPath();
        String query = request.getURI().getQuery();
        String remoteAddr = getClientIP(request);

        String fullPath = query != null ? path + "?" + query : path;

        ACCESS_LOG.info("[GATEWAY] Incoming: {} {} from {} | User: {} | CorrelationID: {}",
                method, fullPath, remoteAddr, username, correlationId);

        // Log headers at debug level
        if (logger.isDebugEnabled()) {
            logger.debug("Request Headers for {}:", fullPath);
            request.getHeaders().forEach((name, values) -> {
                if (!name.equalsIgnoreCase("authorization")) {
                    logger.debug("  {}: {}", name, values);
                }
            });
        }
    }

    private void logResponse(ServerHttpRequest request, ServerHttpResponse response,
                             long duration, String correlationId, String username) {
        String method = request.getMethod().name();
        String path = request.getURI().getPath();
        int status = response.getStatusCode() != null ? response.getStatusCode().value() : 0;

        String level = status >= 500 ? "ERROR" : status >= 400 ? "WARN" : "INFO";

        ACCESS_LOG.info("[GATEWAY] Response: {} {} - Status: {} | Duration: {}ms | User: {} | CorrelationID: {}",
                method, path, status, duration, username, correlationId);

        // Log slow requests
        if (duration > 5000) {
            logger.warn("[GATEWAY] SLOW REQUEST: {} {} took {}ms", method, path, duration);
        }

        // Log failed requests with more detail
        if (status >= 400) {
            logger.warn("[GATEWAY] Failed request: {} {} returned {} after {}ms",
                    method, path, status, duration);
        }
    }

    private void logError(ServerHttpRequest request, Throwable error,
                          long duration, String correlationId, String username) {
        String method = request.getMethod().name();
        String path = request.getURI().getPath();

        ACCESS_LOG.error("[GATEWAY] Request Failed: {} {} | Duration: {}ms | User: {} | CorrelationID: {} | Error: {}",
                method, path, duration, username, correlationId, error.getMessage());

        logger.error("[GATEWAY] Error processing request: {} {}", method, path, error);
    }

    private String extractUsername(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            return "anonymous";
        }

        try {
            if (authentication.getPrincipal() instanceof Jwt) {
                Jwt jwt = (Jwt) authentication.getPrincipal();

                // Try preferred_username first
                String username = jwt.getClaimAsString("preferred_username");
                if (username != null && !username.isEmpty()) {
                    return username;
                }

                // Fallback to email
                username = jwt.getClaimAsString("email");
                if (username != null && !username.isEmpty()) {
                    return username;
                }

                // Fallback to sub
                return jwt.getClaimAsString("sub");
            }

            return authentication.getName();

        } catch (Exception e) {
            logger.debug("Could not extract username", e);
            return "unknown";
        }
    }

    private String getClientIP(ServerHttpRequest request) {
        HttpHeaders headers = request.getHeaders();

        // Check X-Forwarded-For header first (standard for proxies)
        String ip = headers.getFirst("X-Forwarded-For");
        if (ip != null && !ip.isEmpty() && !"unknown".equalsIgnoreCase(ip)) {
            // X-Forwarded-For can contain multiple IPs, take the first one
            if (ip.contains(",")) {
                ip = ip.split(",")[0].trim();
            }
            return ip;
        }

        // Check other common headers
        String[] headerNames = {
                "X-Real-IP",
                "Proxy-Client-IP",
                "WL-Proxy-Client-IP"
        };

        for (String headerName : headerNames) {
            ip = headers.getFirst(headerName);
            if (ip != null && !ip.isEmpty() && !"unknown".equalsIgnoreCase(ip)) {
                return ip;
            }
        }

        // Fallback to remote address
        if (request.getRemoteAddress() != null) {
            return request.getRemoteAddress().getAddress().getHostAddress();
        }

        return "unknown";
    }

    private Authentication createAnonymousAuth() {
        return new Authentication() {
            @Override
            public String getName() {
                return "anonymous";
            }

            @Override
            public Object getCredentials() {
                return null;
            }

            @Override
            public Object getDetails() {
                return null;
            }

            @Override
            public Object getPrincipal() {
                return "anonymous";
            }

            @Override
            public boolean isAuthenticated() {
                return false;
            }

            @Override
            public void setAuthenticated(boolean isAuthenticated) throws IllegalArgumentException {
            }

            @Override
            public java.util.Collection<? extends org.springframework.security.core.GrantedAuthority> getAuthorities() {
                return java.util.Collections.emptyList();
            }
        };
    }

    @Override
    public int getOrder() {
        return Ordered.HIGHEST_PRECEDENCE;
    }
}