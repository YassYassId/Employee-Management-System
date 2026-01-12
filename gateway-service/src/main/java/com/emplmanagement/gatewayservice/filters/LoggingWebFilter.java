package com.emplmanagement.gatewayservice.filters;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;

import java.security.Principal;
import java.time.Instant;
import java.util.UUID;

@Component
public class LoggingWebFilter implements WebFilter {

    private static final Logger log = LoggerFactory.getLogger(LoggingWebFilter.class);
    private static final String CORRELATION_ID_HEADER = "X-Correlation-Id";

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {
        ServerHttpRequest request = exchange.getRequest();
        ServerHttpResponse response = exchange.getResponse();

        String correlationId = request.getHeaders().getFirst(CORRELATION_ID_HEADER);
        if (correlationId == null || correlationId.isBlank()) {
            correlationId = UUID.randomUUID().toString();
        }
        MDC.put("correlationId", correlationId);

        exchange.getResponse().getHeaders().add(CORRELATION_ID_HEADER, correlationId);

        Instant start = Instant.now();

        return exchange.getPrincipal()
                .defaultIfEmpty((Principal) () -> "anonymous")
                .flatMap(principal -> {
                    String username = principal != null ? principal.getName() : "anonymous";
                    MDC.put("username", username);

                    String method = request.getMethod() != null ? request.getMethod().name() : "UNKNOWN";
                    String path = request.getURI().getPath();
                    String query = request.getURI().getQuery();
                    String clientIp = request.getRemoteAddress() != null ? request.getRemoteAddress().getAddress().getHostAddress() : "unknown";

                    return chain.filter(exchange)
                            .doOnSuccess(v -> {
                                long durationMs = java.time.Duration.between(start, Instant.now()).toMillis();
                                int status = response.getStatusCode() != null ? response.getStatusCode().value() : 200;
                                log.info("ACCESS gateway method={} path={}{} status={} durationMs={} clientIp={}",
                                        method,
                                        path,
                                        (query != null ? ("?" + query) : ""),
                                        status,
                                        durationMs,
                                        clientIp);
                            })
                            .doOnError(ex -> {
                                long durationMs = java.time.Duration.between(start, Instant.now()).toMillis();
                                int status = response.getStatusCode() != null ? response.getStatusCode().value() : 500;
                                log.error("ERROR gateway method={} path={} status={} durationMs={} error={}",
                                        method, path, status, durationMs, ex.toString());
                            })
                            .doFinally(sig -> {
                                MDC.remove("username");
                                MDC.remove("correlationId");
                                MDC.remove("traceId");
                            });
                });
    }
}
