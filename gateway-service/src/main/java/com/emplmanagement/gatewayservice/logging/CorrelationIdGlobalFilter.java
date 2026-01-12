package com.emplmanagement.gatewayservice.logging;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpHeaders;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.ReactiveSecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

/**
 * Global filter for access logging, correlation ID propagation and user identification in logs.
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class CorrelationIdGlobalFilter implements GlobalFilter {

    public static final String CORRELATION_ID_HEADER = "X-Correlation-Id";
    private static final Logger ACCESS_LOG = LoggerFactory.getLogger("ACCESS_LOG");

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        final Instant start = Instant.now();
        String existingCorrelationId = exchange.getRequest().getHeaders().getFirst(CORRELATION_ID_HEADER);
        final String correlationId = Optional.ofNullable(existingCorrelationId).orElse(UUID.randomUUID().toString());

        // Ensure header exists for downstream services
        exchange.getRequest().mutate().headers(httpHeaders -> httpHeaders.set(CORRELATION_ID_HEADER, correlationId)).build();

        return ReactiveSecurityContextHolder.getContext()
                .map(sc -> sc.getAuthentication())
                .defaultIfEmpty(null)
                .flatMap(auth -> {
                    String username = extractUsername(auth).orElse("anonymous");
                    // Populate MDC for this reactive context
                    MDC.put("correlationId", correlationId);
                    MDC.put("user", username);

                    HttpHeaders headers = exchange.getRequest().getHeaders();
                    String method = String.valueOf(exchange.getRequest().getMethod());
                    String path = exchange.getRequest().getURI().getRawPath();
                    String userAgent = headers.getFirst(HttpHeaders.USER_AGENT);
                    String ip = Optional.ofNullable(headers.getFirst("X-Forwarded-For"))
                            .orElseGet(() -> Optional.ofNullable(exchange.getRequest().getRemoteAddress())
                                    .map(ra -> ra.getAddress().getHostAddress()).orElse("unknown"));

                    return chain.filter(exchange).doOnSuccess(v -> {
                        long status = exchange.getResponse().getStatusCode() != null ? exchange.getResponse().getStatusCode().value() : 0;
                        long durationMs = Duration.between(start, Instant.now()).toMillis();
                        ACCESS_LOG.info("method={} path={} status={} duration_ms={} user={} ip={} ua=\"{}\" corrId={}",
                                method, path, status, durationMs, username, ip, userAgent, correlationId);
                    }).doOnError(err -> {
                        long durationMs = Duration.between(start, Instant.now()).toMillis();
                        ACCESS_LOG.error("method={} path={} status=500 duration_ms={} user={} ip={} ua=\"{}\" corrId={} error={}",
                                method, path, durationMs, username, ip, userAgent, correlationId, err.getMessage());
                    }).doFinally(s -> {
                        // clear reactive MDC
                        MDC.remove("correlationId");
                        MDC.remove("user");
                    });
                });
    }

    private Optional<String> extractUsername(Authentication authentication) {
        if (authentication == null) return Optional.empty();
        Object principal = authentication.getPrincipal();
        if (principal instanceof Jwt jwt) {
            Object preferred = jwt.getClaims().getOrDefault("preferred_username", jwt.getSubject());
            return Optional.ofNullable(preferred).map(Object::toString);
        }
        return Optional.ofNullable(authentication.getName());
    }
}
